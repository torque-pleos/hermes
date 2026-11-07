package com.zigae.hermes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zigae.hermes.fleet.FleetFusion
import com.zigae.hermes.fleet.FleetState
import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePointRain
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviSpace
import com.zigae.hermes.ui.theme.IviType
import com.zigae.hermes.vehicle.VehicleSnapshot
import com.zigae.hermes.vehicle.toRain
import com.zigae.hermes.vehicle.toRainLevel

/** 차량 공유: 주변 관측 현황 · 공유 동의(큰 스위치) · 내 와이퍼 · 시연 */
@Composable
fun FleetScreen(
    fleet: FleetState,
    vehicle: VehicleSnapshot,
    routeRain: List<RoutePointRain>,
    onToggleConsent: () -> Unit,
    onSeedDemo: () -> Unit,
) {
    val c = Ivi.colors
    val now = System.currentTimeMillis() / 1000
    val vehicles = fleet.reports.groupBy { it.vehicleId }.mapValues { (_, rs) -> rs.maxBy { it.observedAtEpochSec } }
    val wet = vehicles.values.count { it.level > RainLevel.NONE }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 900.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = IviSpace.xl, vertical = IviSpace.l), verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
            Text("차량 공유", style = IviType.headline, color = c.ivi_text_primary)

            val left: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                    IviCard(Modifier.fillMaxWidth()) {
                        if (!fleet.configured) {
                            Text("공유 서버가 설정되지 않았습니다", style = IviType.title, color = c.ivi_text_secondary)
                            return@IviCard
                        }
                        Text("경로 주변, 최근 20분", style = IviType.label, color = c.ivi_text_secondary)
                        Text(
                            if (vehicles.isEmpty()) "주변에 공유 중인 차량이 없습니다"
                            else if (wet == 0) "${vehicles.size}대 모두 비를 보지 않았습니다"
                            else "${vehicles.size}대 중 ${wet}대가 비를 보고 있습니다",
                            style = IviType.display, color = if (wet > 0) c.ivi_status_caution else c.ivi_text_primary,
                        )
                        if (wet > 0) Row(horizontalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                            listOf(RainLevel.LIGHT, RainLevel.MODERATE, RainLevel.HEAVY, RainLevel.VERY_HEAVY).forEach { l ->
                                val n = vehicles.values.count { it.level == l }
                                if (n > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                                    RainGlyph(l, 40.dp)
                                    Text(" ${l.label} ${n}대", style = IviType.body, color = c.ivi_text_secondary)
                                }
                            }
                        }
                        val adjusted = routeRain.count { (it.forecast as? PointForecast.Fleet)?.let { f -> f.level != f.base.rainLevel } == true }
                        if (adjusted > 0) Chip("경로 예보 ${adjusted}곳을 차량 관측으로 고쳤습니다", c.ivi_status_caution, filled = true)
                        fleet.error?.let { Text(it, style = IviType.caption, color = c.ivi_status_caution) }
                    }
                    if (fleet.configured) BigToggle(
                        title = "내 와이퍼 관측 공유",
                        subtitle = "위치는 약 500m 단위, 차량은 무작위 ID로만 올립니다" +
                            if (fleet.consent && fleet.lastUploadEpochSec > 0) {
                                val ago = (now - fleet.lastUploadEpochSec) / 60
                                if (ago < 1) ". 방금 공유함" else ". ${ago}분 전 공유함"
                            } else "",
                        checked = fleet.consent,
                        onToggle = onToggleConsent,
                    )
                }
            }
            val right: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                    IviCard(Modifier.fillMaxWidth()) {
                        Text("내 차", style = IviType.label, color = c.ivi_text_secondary)
                        val rain = vehicle.wiperSwitch.toRain()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RainGlyph(rain.toRainLevel(), 72.dp)
                            Spacer(Modifier.width(IviSpace.l))
                            Column {
                                Text("와이퍼로 본 비: ${rain.label}", style = IviType.title, color = c.ivi_text_primary)
                                val here = routeRain.firstOrNull()?.forecast?.let { (it as? PointForecast.Fleet)?.base ?: it }
                                Text("와이퍼 ${vehicle.wiperSwitch?.name ?: "-"}, 레이더는 ${here?.rainLevel?.label ?: "-"}", style = IviType.caption, color = c.ivi_text_secondary)
                            }
                        }
                    }
                    Column(Modifier.padding(horizontal = IviSpace.s), verticalArrangement = Arrangement.spacedBy(IviSpace.s)) {
                        Text("이렇게 판단합니다", style = IviType.label, color = c.ivi_text_secondary)
                        Text(
                            "주변 차량의 와이퍼 관측을 비구름이 움직이는 방향으로 도착 시각까지 옮깁니다. " +
                                "반경 ${(FleetFusion.RADIUS_M / 1000).toInt()}km 안에서 ${FleetFusion.MIN_WET}대 이상이 비를 보면 예보를 올리고, " +
                                "${FleetFusion.MIN_DRY}대 이상이 맑으면 레이더의 약한 비 오경보를 지웁니다.",
                            style = IviType.body, color = c.ivi_text_secondary,
                        )
                    }
                    if (fleet.configured) IviButton("시연: 경로 앞에 비 관측 차량 만들기", onSeedDemo, primary = false, icon = HermesIcons.CloudUpload)
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(IviSpace.l)) { left(Modifier.weight(1f)); right(Modifier.weight(1f)) }
            } else {
                left(Modifier.fillMaxWidth()); right(Modifier.fillMaxWidth())
            }
        }
    }
}
