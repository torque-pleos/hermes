package com.zigae.hermes.radar

import kotlin.math.floor

/** 타일 좌표 범위 (양 끝 포함). */
data class TileRange(val zoom: Int, val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
    val columns get() = maxX - minX + 1
    val rows get() = maxY - minY + 1
    val count get() = columns * rows

    fun tiles(): List<Pair<Int, Int>> =
        (minY..maxY).flatMap { y -> (minX..maxX).map { x -> x to y } }

}

/**
 * 경로 지점별로 필요한 타일만 고른다.
 * - 외삽 한계 이내 지점: 비구름이 그 시간 동안 이동해 올 거리(최대 [maxSpeedKmh])만큼 여유
 * - 한계를 넘는 지점: 현재 관측만 쓰므로 지점 주변 소량
 * 타일이 [maxTiles]를 넘으면 가까운 시각 지점의 타일을 우선한다.
 */
object RouteTiles {
    data class Need(val lat: Double, val lon: Double, val leadMinutes: Int)

    fun select(
        needs: List<Need>,
        zoom: Int,
        horizonMinutes: Int,
        maxTiles: Int,
        maxSpeedKmh: Double = 100.0,
    ): Set<Pair<Int, Int>> {
        val size = WebMercator.TILE_SIZE
        val last = (1 shl zoom) - 1
        val picked = LinkedHashSet<Pair<Int, Int>>()
        for (n in needs.sortedBy { it.leadMinutes }) {
            val mpp = WebMercator.metersPerPixel(n.lat, zoom)
            val reachMin = n.leadMinutes.coerceIn(0, horizonMinutes)
            val marginPx = if (n.leadMinutes > horizonMinutes) BASE_MARGIN_PX
            else (BASE_MARGIN_PX + reachMin * maxSpeedKmh * 1000 / 60 / mpp).toInt()
            val px = WebMercator.pixelX(n.lon, zoom)
            val py = WebMercator.pixelY(n.lat, zoom)
            val tiles = buildList {
                for (ty in clampInt(floor((py - marginPx) / size).toInt(), 0, last)..clampInt(floor((py + marginPx) / size).toInt(), 0, last))
                    for (tx in clampInt(floor((px - marginPx) / size).toInt(), 0, last)..clampInt(floor((px + marginPx) / size).toInt(), 0, last))
                        add(tx to ty)
            }
            for (t in tiles) {
                if (picked.size >= maxTiles) return picked
                picked += t
            }
        }
        return picked
    }

    fun bounds(zoom: Int, tiles: Set<Pair<Int, Int>>): TileRange? {
        if (tiles.isEmpty()) return null
        return TileRange(zoom, tiles.minOf { it.first }, tiles.minOf { it.second }, tiles.maxOf { it.first }, tiles.maxOf { it.second })
    }

    private const val BASE_MARGIN_PX = 8
}

object RadarMosaic {
    /**
     * 타일 픽셀(ARGB, 256×256)을 이어 붙여 [RadarFrame]을 만든다. 없는 타일은 [RadarFrame.NO_DATA]로 채운다.
     */
    fun build(timeEpochSec: Long, range: TileRange, tiles: Map<Pair<Int, Int>, IntArray>): RadarFrame {
        val size = WebMercator.TILE_SIZE
        val w = range.columns * size
        val h = range.rows * size
        val dbz = FloatArray(w * h) { RadarFrame.NO_DATA }
        for ((key, pixels) in tiles) {
            val (tx, ty) = key
            if (tx !in range.minX..range.maxX || ty !in range.minY..range.maxY) continue
            require(pixels.size == size * size) { "tile $key has ${pixels.size} pixels" }
            val ox = (tx - range.minX) * size
            val oy = (ty - range.minY) * size
            for (y in 0 until size) {
                val row = (oy + y) * w + ox
                for (x in 0 until size) dbz[row + x] = RainViewerPalette.decode(pixels[y * size + x])
            }
        }
        return RadarFrame(timeEpochSec, range.zoom, range.minX * size, range.minY * size, w, h, dbz)
    }
}
