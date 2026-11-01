package com.zigae.hermes.fleet

import com.zigae.hermes.geo.GeoMath
import com.zigae.hermes.geo.LatLon
import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.PointForecaster
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePoint
import kotlin.math.cos

/** Hermes를 쓰는 차량 한 대의 와이퍼 관측 */
data class FleetReport(
    val vehicleId: String,
    val lat: Double,
    val lon: Double,
    val observedAtEpochSec: Long,
    /** 와이퍼 단계로 추정한 강수 (AUTO 등 알 수 없는 단계는 올리지 않는다) */
    val level: RainLevel,
    val demo: Boolean = false,
)

/** 한 지점·시각에 대한 주변 차량 근거 */
data class FleetEvidence(
    val vehicles: Int,
    val wetVehicles: Int,
    val dryVehicles: Int,
    /** 비를 본 차량이 2대 이상일 때의 대표 강도(두 번째로 강한 값 — 한 대의 이상값에 휘둘리지 않게) */
    val consensus: RainLevel?,
)

/** 지점의 비구름 이동 속도(동쪽·북쪽 m/s). 모르면 null (정지로 본다) */
fun interface Wind {
    fun at(lat: Double, lon: Double): Pair<Double, Double>?
}

/**
 * 주변 차량 와이퍼 관측(크라우드 관측)을 레이더·기상청 예보와 합친다.
 *
 * - 관측은 [MAX_AGE_SEC] 이내, 지금부터 [MAX_LEAD_MIN] 이내 지점에만 쓴다(그 뒤는 관측이 의미 없음).
 * - 관측 위치를 비구름 이동 속도로 도착 시각까지 옮긴 뒤 반경 [RADIUS_M] 안이면 근거로 센다.
 * - 차량마다 가장 최근 관측 하나만 센다.
 * - 비를 본 차량 ≥ 2: 예보 강도와 차량 합의 강도 중 큰 값 (레이더가 놓친 국지성 비·이슬비 보완)
 * - 맑음 차량 ≥ 3 이고 비를 본 차량 0, 예보가 약한 비 이하: 비 없음 (레이더 오경보 억제)
 */
object FleetFusion {
    const val RADIUS_M = 8_000.0
    const val MAX_AGE_SEC = 20 * 60L
    const val MAX_LEAD_MIN = 60
    const val MIN_WET = 2
    const val MIN_DRY = 3

    fun evidence(
        reports: List<FleetReport>,
        target: RoutePoint,
        nowEpochSec: Long,
        wind: Wind?,
    ): FleetEvidence? {
        if ((target.etaEpochSec - nowEpochSec) / 60 > MAX_LEAD_MIN) return null
        val latest = reports
            .filter { nowEpochSec - it.observedAtEpochSec in 0..MAX_AGE_SEC }
            .groupBy { it.vehicleId }
            .mapNotNull { (_, rs) -> rs.maxByOrNull { it.observedAtEpochSec } }
        val near = latest.filter { r ->
            val dt = (target.etaEpochSec - r.observedAtEpochSec).toDouble()
            val moved = advect(r.lat, r.lon, dt, wind?.at(r.lat, r.lon))
            GeoMath.distanceMeters(moved, LatLon(target.lat, target.lon)) <= RADIUS_M
        }
        if (near.isEmpty()) return null
        val wet = near.filter { it.level > RainLevel.NONE }.map { it.level }.sortedDescending()
        return FleetEvidence(
            vehicles = near.size,
            wetVehicles = wet.size,
            dryVehicles = near.size - wet.size,
            consensus = if (wet.size >= MIN_WET) wet[1] else null,
        )
    }

    fun fuse(base: PointForecast, ev: FleetEvidence?): PointForecast {
        if (ev == null) return base
        val baseLevel = base.rainLevel
        val level = when {
            ev.consensus != null -> maxOf(baseLevel ?: RainLevel.NONE, ev.consensus)
            ev.wetVehicles == 0 && ev.dryVehicles >= MIN_DRY &&
                (baseLevel == null || baseLevel <= RainLevel.LIGHT) -> RainLevel.NONE
            else -> baseLevel
        }
        return PointForecast.Fleet(level, base, ev)
    }

    /** 동쪽·북쪽 속도(m/s)로 dt초 이동한 위치 */
    fun advect(lat: Double, lon: Double, dtSec: Double, velocity: Pair<Double, Double>?): LatLon {
        if (velocity == null || dtSec == 0.0) return LatLon(lat, lon)
        val (east, north) = velocity
        val dLat = north * dtSec / 111_320.0
        val dLon = east * dtSec / (111_320.0 * cos(Math.toRadians(lat)))
        return LatLon(lat + dLat, lon + dLon)
    }
}

/** 기존 예측기(레이더+기상청)에 주변 차량 관측을 덧입힌다 */
class FleetForecaster(
    private val inner: PointForecaster,
    private val reports: List<FleetReport>,
    private val nowEpochSec: Long,
    private val wind: Wind?,
) : PointForecaster {
    override fun forecast(point: RoutePoint): PointForecast =
        FleetFusion.fuse(inner.forecast(point), FleetFusion.evidence(reports, point, nowEpochSec, wind))
}
