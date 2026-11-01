package com.zigae.hermes.geo

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double)

object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_008.8

    fun distanceMeters(a: LatLon, b: LatLon): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }

    /** a→b 대권 위의 비율 f(0..1) 지점 */
    fun interpolate(a: LatLon, b: LatLon, f: Double): LatLon {
        val d = distanceMeters(a, b) / EARTH_RADIUS_M
        if (d < 1e-9) return a
        val la1 = Math.toRadians(a.lat); val lo1 = Math.toRadians(a.lon)
        val la2 = Math.toRadians(b.lat); val lo2 = Math.toRadians(b.lon)
        val s1 = sin((1 - f) * d) / sin(d)
        val s2 = sin(f * d) / sin(d)
        val x = s1 * cos(la1) * cos(lo1) + s2 * cos(la2) * cos(lo2)
        val y = s1 * cos(la1) * sin(lo1) + s2 * cos(la2) * sin(lo2)
        val z = s1 * sin(la1) + s2 * sin(la2)
        return LatLon(Math.toDegrees(atan2(z, sqrt(x * x + y * y))), Math.toDegrees(atan2(y, x)))
    }
}

/**
 * 지오코딩 후보 중 내비 남은 거리와 맞는 후보를 고른다.
 * 도로 거리는 직선 거리의 1.0~2.5배라고 보고, 그 범위 안에서 전형적 비율(1.25)에 가장 가까운 후보를 택한다.
 * "서구 둔산동"처럼 시·도가 빠진 주소는 여러 도시에서 후보가 나오므로 이 검사가 필요하다.
 */
object CandidatePicker {
    private const val MIN_ROAD_FACTOR = 1.0
    private const val MAX_ROAD_FACTOR = 2.5
    private const val TYPICAL_ROAD_FACTOR = 1.25
    /** 짧은 거리에서는 오차가 커서 허용 여유를 둔다 */
    private const val SLACK_M = 3_000.0

    fun pick(candidates: List<LatLon>, here: LatLon?, remainDistanceM: Int): LatLon? {
        if (candidates.isEmpty()) return null
        if (here == null || remainDistanceM <= 0) return candidates.first()
        return candidates
            .map { it to GeoMath.distanceMeters(here, it) }
            .filter { (_, straight) ->
                remainDistanceM + SLACK_M >= straight * MIN_ROAD_FACTOR &&
                    remainDistanceM <= straight * MAX_ROAD_FACTOR + SLACK_M
            }
            .minByOrNull { (_, straight) -> kotlin.math.abs(remainDistanceM - straight * TYPICAL_ROAD_FACTOR) }
            ?.first
    }
}
