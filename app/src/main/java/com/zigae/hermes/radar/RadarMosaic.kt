package com.zigae.hermes.radar

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** 타일 좌표 범위 (양 끝 포함). */
data class TileRange(val zoom: Int, val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
    val columns get() = maxX - minX + 1
    val rows get() = maxY - minY + 1
    val count get() = columns * rows

    fun tiles(): List<Pair<Int, Int>> =
        (minY..maxY).flatMap { y -> (minX..maxX).map { x -> x to y } }

    companion object {
        /**
         * 관심 지점들을 모두 덮고, 비구름이 외삽 시간 동안 이동해 올 거리만큼 여유를 둔 타일 범위.
         * 타일 수가 [maxTiles]를 넘으면 첫 지점(현재 위치) 중심으로 줄인다.
         */
        fun covering(
            points: List<Pair<Double, Double>>,
            zoom: Int,
            marginPx: Int,
            maxTiles: Int,
        ): TileRange? {
            if (points.isEmpty()) return null
            val xs = points.map { WebMercator.pixelX(it.second, zoom) }
            val ys = points.map { WebMercator.pixelY(it.first, zoom) }
            val size = WebMercator.TILE_SIZE
            val last = (1 shl zoom) - 1
            fun tile(px: Double) = floor(px / size).toInt()

            var range = TileRange(
                zoom,
                clampInt(tile(xs.min() - marginPx), 0, last),
                clampInt(tile(ys.min() - marginPx), 0, last),
                clampInt(tile(xs.max() + marginPx), 0, last),
                clampInt(tile(ys.max() + marginPx), 0, last),
            )
            if (range.count <= maxTiles) return range

            // 현재 위치 중심으로 축소
            val side = floor(Math.sqrt(maxTiles.toDouble())).toInt().coerceAtLeast(1)
            val cx = tile(xs.first())
            val cy = tile(ys.first())
            val half = side / 2
            val minX = max(range.minX, min(cx - half, range.maxX - side + 1))
            val minY = max(range.minY, min(cy - half, range.maxY - side + 1))
            range = TileRange(zoom, minX, minY, min(minX + side - 1, last), min(minY + side - 1, last))
            return range
        }
    }
}

object RadarMosaic {
    /**
     * 타일 픽셀(ARGB, 256×256)을 이어 붙여 [RadarFrame]을 만든다. 없는 타일은 에코 없음으로 채운다.
     */
    fun build(timeEpochSec: Long, range: TileRange, tiles: Map<Pair<Int, Int>, IntArray>): RadarFrame {
        val size = WebMercator.TILE_SIZE
        val w = range.columns * size
        val h = range.rows * size
        val dbz = FloatArray(w * h) { RadarFrame.NO_ECHO }
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
