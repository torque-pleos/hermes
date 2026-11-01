package com.zigae.hermes.radar

import com.zigae.hermes.geo.GeoMath
import com.zigae.hermes.geo.LatLon
import kotlin.math.ceil

/** 강수 예측 대상 지점 (현재 위치·경유지·목적지). */
data class RoutePoint(
    val label: String,
    val lat: Double,
    val lon: Double,
    /** 도착 예정 시각(epoch 초). 현재 위치는 지금. */
    val etaEpochSec: Long,
)

data class RoutePointRain(val point: RoutePoint, val forecast: PointForecast)

/** 두 지점 사이 구간의 강수 요약 */
data class LegRain(
    val from: RoutePoint,
    val to: RoutePoint,
    /** 구간에서 가장 강한 예측 강수와 그 표본 지점 (예측 가능한 표본이 없으면 null) */
    val worst: RoutePointRain?,
    val samples: List<RoutePointRain>,
) {
    /** 예측 한계를 넘은 표본 수 */
    val beyondHorizon get() = samples.count { it.forecast is PointForecast.BeyondHorizon }
}

object RouteRain {
    /** 구간 표본 간격(직선 거리) */
    const val SAMPLE_SPACING_M = 15_000.0

    fun forecast(nowcaster: Nowcaster, points: List<RoutePoint>): List<RoutePointRain> =
        points.map { RoutePointRain(it, nowcaster.forecast(it.lat, it.lon, it.etaEpochSec)) }

    /**
     * 지점 사이를 직선(대권)으로 잇고 [SAMPLE_SPACING_M]마다 표본을 둔다. 표본 도착 시각은 구간 소요 시간을
     * 직선 거리 비율로 나눠 추정한다. NaviHelper가 경로 좌표열을 주지 않아 실제 도로와 다를 수 있는 근사다.
     */
    fun samplePath(points: List<RoutePoint>): List<List<RoutePoint>> =
        points.zipWithNext().map { (a, b) ->
            val pa = LatLon(a.lat, a.lon)
            val pb = LatLon(b.lat, b.lon)
            val n = ceil(GeoMath.distanceMeters(pa, pb) / SAMPLE_SPACING_M).toInt().coerceAtLeast(1)
            (0..n).map { i ->
                val f = i.toDouble() / n
                val p = GeoMath.interpolate(pa, pb, f)
                RoutePoint(
                    label = when (i) { 0 -> a.label; n -> b.label; else -> "${a.label}→${b.label}" },
                    lat = p.lat, lon = p.lon,
                    etaEpochSec = a.etaEpochSec + ((b.etaEpochSec - a.etaEpochSec) * f).toLong(),
                )
            }
        }

    fun legs(nowcaster: Nowcaster, points: List<RoutePoint>): List<LegRain> =
        samplePath(points).map { path ->
            val samples = forecast(nowcaster, path)
            val worst = samples
                .filter { it.forecast is PointForecast.Rain }
                .maxWithOrNull(compareBy<RoutePointRain> { (it.forecast as PointForecast.Rain).level }
                    .thenBy { (it.forecast as PointForecast.Rain).dbz })
            LegRain(path.first(), path.last(), worst, samples)
        }
}
