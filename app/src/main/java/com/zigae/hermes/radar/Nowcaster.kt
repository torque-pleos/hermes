package com.zigae.hermes.radar

import kotlin.math.roundToInt

/** 한 지점·시각의 강수 예측 결과 */
sealed interface PointForecast {
    data class Rain(
        val level: RainLevel,
        val dbz: Float,
        val rateMmPerHour: Double,
        /** 레이더 관측 시각으로부터의 외삽 시간(분) */
        val leadMinutes: Int,
    ) : PointForecast

    /** 예측 가능 시간(외삽 한계)을 넘음 */
    data class BeyondHorizon(val leadMinutes: Int) : PointForecast

    /** 레이더 영역 밖 */
    data object OutOfCoverage : PointForecast
}

/**
 * 최신 레이더 프레임을 추정 이동 벡터로 평행 이동시키는 외삽 예측(semi-Lagrangian advection).
 * RainViewer는 2026-01-01부터 nowcast를 제공하지 않아 직접 계산한다.
 */
class Nowcaster(
    val latest: RadarFrame,
    val motion: Motion,
    private val horizonMinutes: Int = DEFAULT_HORIZON_MIN,
) {
    fun forecast(lat: Double, lon: Double, atEpochSec: Long): PointForecast {
        val leadSec = (atEpochSec - latest.timeEpochSec).coerceAtLeast(0)
        val leadMin = (leadSec / 60).toInt()
        if (leadMin > horizonMinutes) return PointForecast.BeyondHorizon(leadMin)

        // t+lead에 (x,y)에 있을 비는 지금 (x - v·lead, y - v·lead)에 있다.
        val sx = latest.gridX(lon) - motion.vxPxPerSec * leadSec
        val sy = latest.gridY(lat) - motion.vyPxPerSec * leadSec
        // 외삽 시간이 길수록 위치 불확실성이 커지므로 탐색 반경을 넓힌다.
        val radius = BASE_RADIUS_PX + (leadMin / 15.0).roundToInt()
        val dbz = latest.maxAround(sx, sy, radius) ?: return PointForecast.OutOfCoverage
        return PointForecast.Rain(RainLevel.fromDbz(dbz), dbz, RainLevel.rainRateMmPerHour(dbz), leadMin)
    }

    companion object {
        const val DEFAULT_HORIZON_MIN = 90
        private const val BASE_RADIUS_PX = 2
    }
}
