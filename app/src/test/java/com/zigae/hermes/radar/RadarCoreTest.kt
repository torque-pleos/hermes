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
    fun decodesBlackWhiteColorScheme() {
        assertEquals(RadarFrame.NO_ECHO, RadarFrame.decodeBlackWhite(0x00000000))
        assertEquals(0f, RadarFrame.decodeBlackWhite(argb(0x20))) // #202020 → 0 dBZ
        assertEquals(40f, RadarFrame.decodeBlackWhite(argb(0x48))) // #484848 → 40 dBZ
        assertEquals(95f, RadarFrame.decodeBlackWhite(argb(0xff))) // 눈 영역 #ffffff → 95 dBZ
        assertTrue(RadarFrame.isSnow(argb(0xa0)))
        assertEquals(0f, RadarFrame.decodeBlackWhite(argb(0xa0))) // 눈 #a0a0a0 → 0 dBZ
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

        // 예측 한계 초과
        assertTrue(cast.forecast(lat, lon, atEpochSec = 120 * 60) is PointForecast.BeyondHorizon)
    }

    @Test
    fun outsideGridIsOutOfCoverage() {
        val latest = blobFrame(time = 0, cx = 60, cy = 80)
        val (lat, lon) = latLonOf(latest, -50.0, 80.0)
        assertTrue(Nowcaster(latest, Motion.NONE).forecast(lat, lon, 0) is PointForecast.OutOfCoverage)
    }

    private fun argb(grey: Int) = (0xFF shl 24) or (grey shl 16) or (grey shl 8) or grey

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
