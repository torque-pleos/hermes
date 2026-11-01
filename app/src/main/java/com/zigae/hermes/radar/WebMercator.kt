package com.zigae.hermes.radar

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

/** 256px 타일 기준 Web Mercator 전역 픽셀 좌표 변환 (RainViewer 타일과 동일 체계). */
object WebMercator {
    const val TILE_SIZE = 256
    private const val EARTH_CIRCUMFERENCE_M = 40_075_016.686

    fun worldSize(zoom: Int): Double = TILE_SIZE.toDouble() * (1 shl zoom)

    fun pixelX(lon: Double, zoom: Int): Double = (lon + 180.0) / 360.0 * worldSize(zoom)

    fun pixelY(lat: Double, zoom: Int): Double {
        val rad = Math.toRadians(lat)
        return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * worldSize(zoom)
    }

    fun lon(pixelX: Double, zoom: Int): Double = pixelX / worldSize(zoom) * 360.0 - 180.0

    fun lat(pixelY: Double, zoom: Int): Double {
        val n = PI - 2.0 * PI * pixelY / worldSize(zoom)
        return Math.toDegrees(atan(0.5 * (exp(n) - exp(-n))))
    }

    /** 해당 위도에서 1픽셀이 나타내는 지상 거리(m). */
    fun metersPerPixel(lat: Double, zoom: Int): Double =
        EARTH_CIRCUMFERENCE_M * cos(Math.toRadians(lat)) / worldSize(zoom)
}
