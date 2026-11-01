package com.zigae.hermes.radar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RadarCoreTest {

    @Test
    fun mercatorRoundTrip() {
        val lat = 37.41272
        val lon = 127.09468
        val x = WebMercator.pixelX(lon, 7)
        val y = WebMercator.pixelY(lat, 7)
        assertEquals(109, (x / 256).toInt()) // 한국 중부는 z7 타일 x=109
        assertEquals(lon, WebMercator.lon(x, 7), 1e-9)
        assertEquals(lat, WebMercator.lat(y, 7), 1e-9)
        // z7, 위도 37°에서 1px ≈ 976m
        assertEquals(976.0, WebMercator.metersPerPixel(lat, 7), 15.0)
    }

    @Test
    fun decodesUniversalBluePalette() {
        assertEquals(RadarFrame.NO_ECHO, RainViewerPalette.decode(0x00000000))
        // 실제 타일에서 가장 많이 나온 색 #0091ca → 22 dBZ, #51c5e8 → 17 dBZ
        assertEquals(22f, RainViewerPalette.decode(0xFF0091CA.toInt()))
        assertEquals(17f, RainViewerPalette.decode(0xFF51C5E8.toInt()))
        assertEquals(40f, RainViewerPalette.decode(RainViewerPalette.rainColor(40)))
        assertEquals(RadarFrame.NO_ECHO, RainViewerPalette.decode(0xFF123456.toInt())) // 표에 없는 색
    }

    @Test
    fun rainLevelsFollowMarshallPalmer() {
        assertEquals(RainLevel.NONE, RainLevel.fromDbz(10f))
        assertEquals(RainLevel.LIGHT, RainLevel.fromDbz(25f)) // ≈1.3 mm/h
        assertEquals(RainLevel.MODERATE, RainLevel.fromDbz(35f)) // ≈5.6 mm/h
        assertEquals(RainLevel.HEAVY, RainLevel.fromDbz(44f)) // ≈20 mm/h
        assertEquals(RainLevel.VERY_HEAVY, RainLevel.fromDbz(50f)) // ≈49 mm/h
    }

    @Test
    fun estimatesMotionOfShiftedRainCell() {
        // 10분마다 동쪽 +6px, 남쪽 +3px 이동하는 비구름
        val frames = (0..3).map { i -> blobFrame(time = 600L * i, cx = 60 + 6 * i, cy = 80 + 3 * i) }
        val m = MotionEstimator.estimate(frames)
        assertEquals(6.0 / 600, m.vxPxPerSec, 1e-6)
        assertEquals(3.0 / 600, m.vyPxPerSec, 1e-6)
        assertTrue("confidence=${m.confidence}", m.confidence > 0.3)
        // 동남동 방향(약 117°)
        assertEquals(116.6, m.headingDeg(), 1.0)
    }

    @Test
    fun noMotionWithoutEnoughEcho() {
        val empty = { t: Long -> RadarFrame(t, 7, 0, 0, 64, 64, FloatArray(64 * 64) { RadarFrame.NO_ECHO }) }
        assertEquals(Motion.NONE, MotionEstimator.estimate(listOf(empty(0), empty(600))))
    }

    @Test
    fun nowcastAdvectsRainToDownstreamPoint() {
        val latest = blobFrame(time = 0, cx = 60, cy = 80)
        val motion = Motion(6.0 / 600, 0.0, 1.0) // 10분에 6px 동쪽
        val cast = Nowcaster(latest, motion)

        // 30분 뒤 비구름 중심은 (78, 80)
        val (lat, lon) = latLonOf(latest, 78.0, 80.0)
        val later = cast.forecast(lat, lon, atEpochSec = 1800)
        assertTrue(later is PointForecast.Rain && later.level == RainLevel.HEAVY && later.leadMinutes == 30)

        // 지금 그 지점은 맑음
        val now = cast.forecast(lat, lon, atEpochSec = 0)
        assertTrue(now is PointForecast.Rain && now.level == RainLevel.NONE)

        // 예측 한계 초과: 그 지점의 현재 관측값을 함께 준다
        val beyond = cast.forecast(lat, lon, atEpochSec = 120 * 60)
        assertTrue(beyond is PointForecast.BeyondHorizon && beyond.observedNow == RainLevel.NONE)
        val (cLat, cLon) = latLonOf(latest, 60.0, 80.0)
        val beyondCenter = cast.forecast(cLat, cLon, atEpochSec = 120 * 60)
        assertTrue(beyondCenter is PointForecast.BeyondHorizon && beyondCenter.observedNow == RainLevel.HEAVY)
    }

    @Test
    fun outsideGridIsOutOfCoverage() {
        val latest = blobFrame(time = 0, cx = 60, cy = 80)
        val (lat, lon) = latLonOf(latest, -50.0, 80.0)
        assertTrue(Nowcaster(latest, Motion.NONE).forecast(lat, lon, 0) is PointForecast.OutOfCoverage)
    }

    private fun latLonOf(f: RadarFrame, gx: Double, gy: Double): Pair<Double, Double> =
        WebMercator.lat(f.originY + gy, f.zoom) to WebMercator.lon(f.originX + gx, f.zoom)

    /** 중심 44dBZ에서 바깥으로 줄어드는 반경 14px 비구름 + 약한 비 띠 */
    private fun blobFrame(time: Long, cx: Int, cy: Int): RadarFrame {
        val w = 192
        val h = 192
        val v = FloatArray(w * h) { RadarFrame.NO_ECHO }
        for (y in 0 until h) for (x in 0 until w) {
            val dx = x - cx
            val dy = y - cy
            val d2 = dx * dx + dy * dy
            if (d2 <= 14 * 14) v[y * w + x] = 44f - d2 / 14f
            // 모양을 비대칭으로 만들어 정합이 유일하게 결정되도록 꼬리를 붙인다.
            if (dx in -30..-15 && abs(dy) <= 3) v[y * w + x] = 25f
        }
        return RadarFrame(time, 7, 109 * 256, 49 * 256, w, h, v)
    }
}

class RadarMosaicTest {

    @Test
    fun mosaicPlacesTilesAndDecodes() {
        val range = TileRange(7, 109, 49, 110, 49)
        val c40 = RainViewerPalette.rainColor(40)
        val frame = RadarMosaic.build(0, range, mapOf((110 to 49) to IntArray(256 * 256) { c40 }))
        assertEquals(512, frame.width)
        assertTrue(frame[10, 10].isNaN()) // 받지 않은 타일 = 데이터 없음
        assertEquals(40f, frame[300, 10]) // 두 번째 타일
        assertEquals(109 * 256, frame.originX)
    }
}

class RouteRainTest {
    @Test
    fun samplesLegsWithInterpolatedEta() {
        val a = RoutePoint("현재", 37.41, 127.09, 0)
        val b = RoutePoint("대전", 36.356, 127.391, 6000) // 직선 ≈120km
        val legs = RouteRain.samplePath(listOf(a, b))
        assertEquals(1, legs.size)
        val path = legs[0]
        assertEquals(10, path.size) // ceil(120/15)=8 → 9..10개 표본
        assertEquals(0L, path.first().etaEpochSec)
        assertEquals(6000L, path.last().etaEpochSec)
        assertTrue(path.zipWithNext().all { (p, q) -> q.etaEpochSec > p.etaEpochSec })
    }

    @Test
    fun legWorstPicksStrongestRainOnTheWay() {
        // 격자 중앙에 강한 비, 두 지점은 비 없는 양 끝 → 구간 최악은 중간 표본
        val w = 400; val h = 64
        val v = FloatArray(w * h) { RadarFrame.NO_ECHO }
        for (y in 0 until h) for (x in 190..210) v[y * w + x] = 45f
        val frame = RadarFrame(0, 7, 109 * 256, 49 * 256, w, h, v)
        fun pt(label: String, gx: Int) = RoutePoint(
            label,
            WebMercator.lat((frame.originY + 32).toDouble(), 7),
            WebMercator.lon((frame.originX + gx).toDouble(), 7), 0,
        )
        val leg = RouteRain.legs(Nowcaster(frame, Motion.NONE), listOf(pt("A", 20), pt("B", 380))).single()
        val worst = leg.worst!!.forecast as PointForecast.Rain
        assertEquals(RainLevel.HEAVY, worst.level)
        assertEquals("A→B", leg.worst!!.point.label)
    }
}

class RouteTilesTest {
    private val now = 0

    @Test
    fun nearPointsGetMarginFarPointsOnlyTheirTile() {
        val seoul = RouteTiles.Need(37.41, 127.09, 0)
        val busanFar = RouteTiles.Need(35.08, 128.82, 280) // 예측 한계 밖 → 주변만
        val tiles = RouteTiles.select(listOf(seoul, busanFar), 7, 90, 32)
        val bx = (WebMercator.pixelX(128.82, 7) / 256).toInt()
        val by = (WebMercator.pixelY(35.08, 7) / 256).toInt()
        assertTrue(bx to by in tiles)
        // 부산은 여유 8px라 타일 1~2개만 추가된다
        val withoutBusan = RouteTiles.select(listOf(seoul), 7, 90, 32)
        assertTrue(tiles.size - withoutBusan.size <= 2)
    }

    @Test
    fun capPrefersEarlierPoints() {
        val near = RouteTiles.Need(37.41, 127.09, 60)
        val far = RouteTiles.Need(33.5, 126.5, 80)
        val tiles = RouteTiles.select(listOf(far, near), 7, 90, 4)
        val nx = (WebMercator.pixelX(127.09, 7) / 256).toInt()
        val ny = (WebMercator.pixelY(37.41, 7) / 256).toInt()
        assertEquals(4, tiles.size)
        assertTrue(nx to ny in tiles)
    }

    @Test
    fun missingTilesAreNotTreatedAsDry() {
        // 오른쪽 타일만 받은 모자이크: 왼쪽 지점은 OutOfCoverage(맑음 아님)
        val range = TileRange(7, 109, 49, 110, 49)
        val c40 = RainViewerPalette.rainColor(40)
        val frame = RadarMosaic.build(0, range, mapOf((110 to 49) to IntArray(256 * 256) { c40 }))
        val cast = Nowcaster(frame, Motion.NONE)
        val leftLat = WebMercator.lat(49 * 256 + 100.0, 7)
        val leftLon = WebMercator.lon(109 * 256 + 100.0, 7)
        assertTrue(cast.forecast(leftLat, leftLon, 0) is PointForecast.OutOfCoverage)
        // 경계 근처 지점은 받은 쪽 값만 쓴다
        val edgeLon = WebMercator.lon(110 * 256 + 1.0, 7)
        val f = cast.forecast(leftLat, edgeLon, 0)
        assertTrue(f is PointForecast.Rain && f.dbz == 40f)
    }

    @Test
    fun motionIgnoresMissingTiles() {
        // 비구름이 이동하는 왼쪽 절반만 데이터가 있고 오른쪽은 없음 → 여전히 이동을 찾는다
        val w = 192; val h = 192
        fun frame(t: Long, cx: Int): RadarFrame {
            val v = FloatArray(w * h) { i -> if (i % w >= 120) RadarFrame.NO_DATA else RadarFrame.NO_ECHO }
            for (y in 0 until h) for (x in 0 until 120) {
                val d2 = (x - cx) * (x - cx) + (y - 90) * (y - 90)
                if (d2 <= 196) v[y * w + x] = 44f - d2 / 14f
                if (x - cx in -30..-15 && kotlin.math.abs(y - 90) <= 3) v[y * w + x] = 25f
            }
            return RadarFrame(t, 7, 0, 0, w, h, v)
        }
        val m = MotionEstimator.estimate((0..3).map { frame(600L * it, 40 + 5 * it) })
        assertEquals(5.0 / 600, m.vxPxPerSec, 1e-6)
    }
}

class MotionFieldTest {
    @Test
    fun fieldSeparatesOpposingCells() {
        // 왼쪽 비구름은 동쪽으로, 오른쪽 비구름은 서쪽으로 이동
        val w = 512; val h = 256
        fun frame(t: Long, i: Int): RadarFrame {
            val v = FloatArray(w * h) { RadarFrame.NO_ECHO }
            fun blob(cx: Int, cy: Int) {
                for (y in 0 until h) for (x in 0 until w) {
                    val d2 = (x - cx) * (x - cx) + (y - cy) * (y - cy)
                    if (d2 <= 28 * 28) v[y * w + x] = 44f - d2 / 40f
                    if (x - cx in -50..-30 && abs(y - cy) <= 4) v[y * w + x] = 25f
                }
            }
            blob(90 + 4 * i, 128)
            blob(420 - 4 * i, 128)
            return RadarFrame(t, 7, 0, 0, w, h, v)
        }
        val frames = (0..3).map { frame(600L * it, it) }
        val global = MotionEstimator.estimate(frames)
        val field = MotionEstimator.estimateField(frames, global.copy(confidence = maxOf(global.confidence, 0.5)))!!
        val (leftVx, _) = field.at(90.0, 128.0)
        val (rightVx, _) = field.at(420.0, 128.0)
        assertTrue("left=$leftVx", leftVx > 0)
        assertTrue("right=$rightVx", rightVx < 0)
    }
}
