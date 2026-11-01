package com.zigae.hermes.weather

import com.zigae.hermes.radar.Nowcaster
import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.PointForecaster
import com.zigae.hermes.radar.RoutePoint

/**
 * 레이더 외삽 + 기상청 예보 혼합.
 * - 지금부터 [radarLeadMinutes] 이내: 레이더 외삽 (hindcast상 단기 정확도가 높음)
 * - 그 이후, 또는 레이더 영역 밖: 기상청 초단기(6h) → 단기(약 3일) 예보의 해당 1시간 칸
 * - 기상청 값이 없으면 레이더 결과(예측 범위 밖 + 현재 관측)를 그대로 쓴다
 */
class BlendedForecaster(
    private val nowcaster: Nowcaster?,
    private val kma: (lat: Double, lon: Double) -> List<KmaHourly>?,
    private val nowEpochSec: Long,
    private val radarLeadMinutes: Int = RADAR_LEAD_MIN,
) : PointForecaster {

    override fun forecast(point: RoutePoint): PointForecast {
        val leadMin = (point.etaEpochSec - nowEpochSec) / 60
        val radar = nowcaster?.forecast(point.lat, point.lon, point.etaEpochSec)
        if (radar != null && leadMin <= radarLeadMinutes && radar !is PointForecast.OutOfCoverage) return radar

        val hour = kma(point.lat, point.lon)
            ?.lastOrNull { it.startEpochSec <= point.etaEpochSec && point.etaEpochSec < it.startEpochSec + 3600 }
        if (hour != null) {
            return PointForecast.Kma(hour.level, hour.rateMmPerHour, hour.startEpochSec, hour.pop, hour.isSnow)
        }
        return radar ?: PointForecast.OutOfCoverage
    }

    companion object {
        const val RADAR_LEAD_MIN = 60
    }
}
