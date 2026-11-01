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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
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
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(IviSpace.xl), verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
            Text("차량 공유", style = IviType.display, color = c.ivi_text_primary)
            Text("Hermes 차량들이 와이퍼로 확인한 실제 비를 서로 나눕니다", style = IviType.title, color = c.ivi_text_secondary)

            val left: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                    IviCard(Modifier.fillMaxWidth(), label = "NEARBY · 경로 주변 최근 20분") {
                        if (!fleet.configured) {
                            Text("공유 서버가 설정되지 않았습니다", style = IviType.title, color = c.ivi_text_secondary)
                            return@IviCard
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${vehicles.size}", style = IviType.hero, color = c.ivi_text_primary)
                            Text(" 대 중 ", style = IviType.title, color = c.ivi_text_secondary)
                            Text("$wet", style = IviType.hero, color = if (wet > 0) c.ivi_status_caution else c.ivi_text_primary)
                            Text(" 대가 비 관측", style = IviType.title, color = c.ivi_text_secondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                            listOf(RainLevel.LIGHT, RainLevel.MODERATE, RainLevel.HEAVY, RainLevel.VERY_HEAVY).forEach { l ->
                                val n = vehicles.values.count { it.level == l }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RainGlyph(l, 36.dp)
                                    Text(" ${l.label} $n", style = IviType.body, color = c.ivi_text_secondary)
                                }
                            }
                        }
                        val adjusted = routeRain.count { (it.forecast as? PointForecast.Fleet)?.let { f -> f.level != f.base.rainLevel } == true }
                        if (adjusted > 0) Chip("경로 예보 ${adjusted}곳을 차량 관측으로 보정 중", c.ivi_status_caution, filled = true)
                        fleet.error?.let { Text(it, style = IviType.caption, color = c.ivi_status_caution) }
                    }
                    if (fleet.configured) BigToggle(
                        title = "내 와이퍼 관측 공유",
                        subtitle = "위치는 약 500m 단위로만, 차량은 무작위 ID로 올립니다" +
                            if (fleet.consent && fleet.lastUploadEpochSec > 0) " · ${(now - fleet.lastUploadEpochSec) / 60}분 전 공유" else "",
                        checked = fleet.consent,
                        onToggle = onToggleConsent,
                    )
                }
            }
            val right: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                    IviCard(Modifier.fillMaxWidth(), label = "MY WIPER · 내 차") {
                        val rain = vehicle.wiperSwitch.toRain()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RainGlyph(rain.toRainLevel(), 72.dp)
                            Spacer(Modifier.width(IviSpace.l))
                            Column {
                                Text(rain.label, style = IviType.headline, color = c.ivi_text_primary)
                                val here = routeRain.firstOrNull()?.forecast?.let { (it as? PointForecast.Fleet)?.base ?: it }
                                Text("와이퍼 ${vehicle.wiperSwitch?.name ?: "-"} · 레이더 판단 ${here?.rainLevel?.label ?: "-"}", style = IviType.caption, color = c.ivi_text_secondary)
                            }
                        }
                    }
                    IviCard(Modifier.fillMaxWidth(), label = "HOW · 판단 방식") {
                        listOf(
                            "주변 차량의 와이퍼 관측을 비구름 이동 방향으로 도착 시각까지 옮깁니다",
                            "반경 ${(FleetFusion.RADIUS_M / 1000).toInt()}km 안에서 ${FleetFusion.MIN_WET}대 이상이 비를 보면 예보를 올립니다",
                            "${FleetFusion.MIN_DRY}대 이상이 맑음이면 레이더의 약한 비 오경보를 내립니다",
                        ).forEach { Text("· $it", style = IviType.body, color = c.ivi_text_secondary) }
                    }
                    if (fleet.configured) IviButton("시연: 경로 앞 가상 차량 비 관측 만들기", onSeedDemo, primary = false, icon = Icons.Rounded.CloudUpload)
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
