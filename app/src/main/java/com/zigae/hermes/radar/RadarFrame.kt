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
    /** 행 우선 dBZ 값. 에코 없음은 [NO_ECHO], 받지 않은 타일은 [NO_DATA](NaN). */
    val dbz: FloatArray,
) {
    init {
        require(dbz.size == width * height) { "dbz size ${dbz.size} != ${width}x$height" }
    }

    operator fun get(x: Int, y: Int): Float =
        if (x in 0 until width && y in 0 until height) dbz[y * width + x] else NO_DATA

    fun contains(x: Int, y: Int) = x in 0 until width && y in 0 until height

    /** 위경도 → 격자 내 좌표(실수). */
    fun gridX(lon: Double): Double = WebMercator.pixelX(lon, zoom) - originX
    fun gridY(lat: Double): Double = WebMercator.pixelY(lat, zoom) - originY

    /** (cx, cy) 중심 반경 [radius] 픽셀 안의 최대 반사도. 데이터가 하나도 없으면 null. */
    fun maxAround(cx: Double, cy: Double, radius: Int): Float? {
        val x0 = cx.roundToInt()
        val y0 = cy.roundToInt()
        if (!contains(x0, y0) || this[x0, y0].isNaN()) return null
        var best: Float? = null
        val r2 = radius * radius
        for (dy in -radius..radius) for (dx in -radius..radius) {
            if (dx * dx + dy * dy > r2) continue
            val v = this[x0 + dx, y0 + dy]
            if (!v.isNaN()) best = if (best == null) v else max(best, v)
        }
        return best
    }

    companion object {
        const val NO_ECHO = -32f
        /** 받지 않은 타일 */
        const val NO_DATA = Float.NaN

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
            return fromRate(rainRateMmPerHour(dbz))
        }

        /** 시간당 강수량 기준. [pty]가 0이 아니면(강수형태 있음) 양이 아주 적어도 약한 비로 본다. */
        fun fromRate(rate: Double, pty: Int = 0): RainLevel {
            if (rate <= 0.0) return if (pty != 0) LIGHT else NONE
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
