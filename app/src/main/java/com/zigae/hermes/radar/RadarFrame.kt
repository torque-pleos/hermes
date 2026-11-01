package com.zigae.hermes.radar

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 한 시각의 레이더 반사도(dBZ) 격자. 타일들을 이어 붙인 직사각형 영역이며
 * 좌표는 [zoom] 레벨의 Web Mercator 전역 픽셀이다.
 */
class RadarFrame(
    val timeEpochSec: Long,
    val zoom: Int,
    /** 격자 (0,0)의 전역 픽셀 좌표 */
    val originX: Int,
    val originY: Int,
    val width: Int,
    val height: Int,
    /** 행 우선 dBZ 값. 에코 없음은 [NO_ECHO]. */
    val dbz: FloatArray,
) {
    init {
        require(dbz.size == width * height) { "dbz size ${dbz.size} != ${width}x$height" }
    }

    operator fun get(x: Int, y: Int): Float =
        if (x in 0 until width && y in 0 until height) dbz[y * width + x] else NO_ECHO

    fun contains(x: Int, y: Int) = x in 0 until width && y in 0 until height

    /** 위경도 → 격자 내 좌표(실수). */
    fun gridX(lon: Double): Double = WebMercator.pixelX(lon, zoom) - originX
    fun gridY(lat: Double): Double = WebMercator.pixelY(lat, zoom) - originY

    /** (cx, cy) 중심 반경 [radius] 픽셀 안의 최대 반사도. 격자 밖이면 null. */
    fun maxAround(cx: Double, cy: Double, radius: Int): Float? {
        val x0 = cx.roundToInt()
        val y0 = cy.roundToInt()
        if (!contains(x0, y0)) return null
        var best = NO_ECHO
        val r2 = radius * radius
        for (dy in -radius..radius) for (dx in -radius..radius) {
            if (dx * dx + dy * dy > r2) continue
            best = max(best, this[x0 + dx, y0 + dy])
        }
        return best
    }

    companion object {
        const val NO_ECHO = -32f

    }
}

/** 기상청 강수 강도 구분 기준(mm/h)에 맞춘 단계. */
enum class RainLevel(val label: String) {
    NONE("비 없음"),
    LIGHT("약한 비"),
    MODERATE("보통 비"),
    HEAVY("강한 비"),
    VERY_HEAVY("매우 강한 비"),
    ;

    companion object {
        /** 지형·잡음 에코를 거르기 위한 최소 반사도. */
        const val MIN_RAIN_DBZ = 15f

        fun fromDbz(dbz: Float): RainLevel {
            if (dbz < MIN_RAIN_DBZ) return NONE
            val rate = rainRateMmPerHour(dbz)
            return when {
                rate < 3.0 -> LIGHT
                rate < 15.0 -> MODERATE
                rate < 30.0 -> HEAVY
                else -> VERY_HEAVY
            }
        }

        /** Marshall–Palmer Z = 200·R^1.6 */
        fun rainRateMmPerHour(dbz: Float): Double =
            if (dbz < MIN_RAIN_DBZ) 0.0 else (10.0.pow(dbz / 10.0) / 200.0).pow(1.0 / 1.6)
    }
}

internal fun clampInt(v: Int, lo: Int, hi: Int) = min(max(v, lo), hi)
