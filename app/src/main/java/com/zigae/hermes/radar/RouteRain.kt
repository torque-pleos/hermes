package com.zigae.hermes.radar

/** 강수 예측 대상 지점 (현재 위치·경유지·목적지). */
data class RoutePoint(
    val label: String,
    val lat: Double,
    val lon: Double,
    /** 도착 예정 시각(epoch 초). 현재 위치는 지금. */
    val etaEpochSec: Long,
)

data class RoutePointRain(val point: RoutePoint, val forecast: PointForecast)

object RouteRain {
    fun forecast(nowcaster: Nowcaster, points: List<RoutePoint>): List<RoutePointRain> =
        points.map { RoutePointRain(it, nowcaster.forecast(it.lat, it.lon, it.etaEpochSec)) }
}
