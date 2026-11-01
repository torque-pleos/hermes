package com.zigae.hermes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zigae.hermes.fleet.FleetState
import com.zigae.hermes.radar.LegRain
import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.RadarState
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePointRain
import com.zigae.hermes.vehicle.VehicleSnapshot
import com.zigae.hermes.vehicle.toRain
import com.zigae.hermes.weather.KmaState
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviSpace
import com.zigae.hermes.ui.theme.IviType
import com.zigae.hermes.ui.theme.rainColor

/** 주차 중 홈: 결론 배너 → 경로 강수 타임라인 → 근거 카드 */
@Composable
fun HomeScreen(
    routeRain: List<RoutePointRain>,
    legs: List<LegRain>,
    radar: RadarState,
    kma: KmaState,
    fleet: FleetState,
    vehicle: VehicleSnapshot,
    onToggleConsent: () -> Unit,
    onSeedDemo: () -> Unit,
    onRequestTestRoute: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val c = Ivi.colors
    val now = System.currentTimeMillis() / 1000
    BoxWithConstraints(Modifier.fillMaxSize().background(c.ivi_background_app)) {
        val wide = maxWidth > 1000.dp
        val main: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                Header(onOpenDiagnostics)
                VerdictBanner(legs, routeRain, loading = radar.nowcaster == null && kma.updatedAtEpochSec == 0L)
                TimelineCard(routeRain, legs, now, onRequestTestRoute)
            }
        }
        val side: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                RadarCard(radar, routeRain)
                FleetCard(fleet, onToggleConsent, onSeedDemo)
                WiperCard(vehicle, routeRain)
                SourcesNote()
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize().padding(IviSpace.xl), horizontalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                main(Modifier.weight(0.62f).verticalScroll(rememberScrollState()))
                side(Modifier.weight(0.38f).verticalScroll(rememberScrollState()))
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(IviSpace.l), verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                main(Modifier)
                side(Modifier)
            }
        }
    }
}

@Composable
private fun Header(onOpenDiagnostics: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("HERMES · 화물 운행 강수 코파일럿", style = IviType.label, color = Ivi.colors.ivi_text_label)
            Text("운행 브리핑", style = IviType.headline, color = Ivi.colors.ivi_text_primary)
        }
        IviButton("진단", onOpenDiagnostics, primary = false)
    }
}

/** 경로 전체의 결론 한 줄 + 근거 배지 */
@Composable
private fun VerdictBanner(legs: List<LegRain>, routeRain: List<RoutePointRain>, loading: Boolean) {
    val c = Ivi.colors
    val worst = legs.mapNotNull { it.worst }
        .filter { (it.forecast.rainLevel ?: RainLevel.NONE) > RainLevel.NONE }
        .maxWithOrNull(compareBy<RoutePointRain> { it.forecast.rainLevel }.thenByDescending { it.point.etaEpochSec })
    val level = worst?.forecast?.rainLevel
    val accent = when {
        loading -> c.ivi_text_label
        level == null -> c.ivi_status_safe
        level >= RainLevel.HEAVY -> c.ivi_status_warning
        level >= RainLevel.MODERATE -> c.ivi_status_caution
        else -> rainColor(level)
    }
    val title = when {
        loading -> "강수 예보 불러오는 중"
        routeRain.isEmpty() -> "위치를 기다리는 중"
        legs.isEmpty() -> "목적지를 설정하면 경로 강수를 알려드립니다"
        worst == null -> "가는 길 비 예보 없음"
        else -> "${hhmm(worst.point.etaEpochSec)} 부근 ${level!!.label}"
    }
    val where = worst?.let { w -> legs.firstOrNull { leg -> leg.samples.any { it === w } } }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(IviSpace.cardRadius))
            .background(accent.copy(alpha = 0.12f))
            .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(IviSpace.cardRadius))
            .padding(IviSpace.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(if (level != null) rainColor(level) else accent.copy(alpha = 0.3f)))
        Spacer(Modifier.width(IviSpace.l))
        Column(Modifier.weight(1f)) {
            Text(title, style = IviType.headline, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            where?.let {
                Text("${short(it.from.label)} → ${short(it.to.label)} 구간", style = IviType.body, color = c.ivi_text_secondary, maxLines = 1)
            }
            worst?.let { Row(Modifier.padding(top = IviSpace.s), horizontalArrangement = Arrangement.spacedBy(IviSpace.s)) { sourceChips(it.forecast) } }
        }
    }
}

@Composable
private fun sourceChips(f: PointForecast) {
    val c = Ivi.colors
    when (f) {
        is PointForecast.Fleet -> {
            Chip("주변 차량 ${f.evidence.wetVehicles}대 관측", c.ivi_status_caution, filled = true)
            sourceChips(f.base)
        }
        is PointForecast.Rain -> Chip("레이더 ${f.leadMinutes}분 예측", c.ivi_accent_primary)
        is PointForecast.Kma -> Chip("기상청 예보" + (f.pop?.let { " · 확률 $it%" } ?: ""), c.ivi_accent_primary)
        else -> {}
    }
}

@Composable
private fun TimelineCard(routeRain: List<RoutePointRain>, legs: List<LegRain>, now: Long, onRequestTestRoute: () -> Unit) {
    IviCard(Modifier.fillMaxWidth(), label = "ROUTE RAIN · 경로 강수") {
        if (legs.isEmpty()) {
            Text("내비게이션에 목적지가 없습니다.", style = IviType.body, color = Ivi.colors.ivi_text_secondary)
            IviButton("시연 경로 설정 (대전 경유 → 부산신항)", onRequestTestRoute)
            return@IviCard
        }
        RainStrip(legs, now)
        Legend()
        routeRain.forEachIndexed { i, p -> StopRow(i, p, now, legs.getOrNull(i - 1)) }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(IviSpace.m), verticalAlignment = Alignment.CenterVertically) {
        listOf(RainLevel.NONE, RainLevel.LIGHT, RainLevel.MODERATE, RainLevel.HEAVY, RainLevel.VERY_HEAVY).forEach { l ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(rainColor(l)))
                Text(" ${l.label}", style = IviType.caption, color = Ivi.colors.ivi_text_secondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, rainColor(null), RoundedCornerShape(4.dp)))
            Text(" 예보 없음", style = IviType.caption, color = Ivi.colors.ivi_text_secondary)
        }
        Text("┆ 60분 이후 기상청", style = IviType.caption, color = Ivi.colors.ivi_text_label)
    }
}

/** 정차지 한 줄: 번호 · 이름 · 도착 시각 · 도착 시 강수 · 직전 구간 최악 강수 */
@Composable
private fun StopRow(index: Int, p: RoutePointRain, now: Long, legBefore: LegRain?) {
    val c = Ivi.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.ivi_surface_card_raised).padding(IviSpace.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(if (index == 0) c.ivi_accent_primary else c.ivi_border_card), contentAlignment = Alignment.Center) {
            Text(if (index == 0) "●" else "$index", style = IviType.bodyStrong, color = if (index == 0) c.ivi_accent_on_primary else c.ivi_text_primary)
        }
        Spacer(Modifier.width(IviSpace.m))
        Column(Modifier.weight(1f)) {
            Text(short(p.point.label, keepKind = true), style = IviType.title, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val eta = p.point.etaEpochSec
            Text(
                if (index == 0) "지금" else "${hhmm(eta)} 도착 · ${relative(eta - now)}",
                style = IviType.body, color = c.ivi_text_secondary,
            )
            legBefore?.worst?.let { w ->
                val l = w.forecast.rainLevel
                if (l != null && l > RainLevel.NONE) {
                    Text("가는 길 ${hhmm(w.point.etaEpochSec)} 부근 ${l.label}", style = IviType.caption, color = rainColor(l))
                }
            }
        }
        RainBadge(p.forecast.rainLevel, arrivalText(p.forecast))
    }
}

private fun arrivalText(f: PointForecast): String = when (f) {
    is PointForecast.BeyondHorizon -> "예보 범위 밖"
    PointForecast.OutOfCoverage -> "정보 없음"
    else -> f.rainLevel?.label ?: "정보 없음"
}

@Composable
private fun RadarCard(radar: RadarState, routeRain: List<RoutePointRain>) {
    val c = Ivi.colors
    IviCard(Modifier.fillMaxWidth(), label = "RADAR · 비구름 이동") {
        val cast = radar.nowcaster
        if (cast == null) {
            Text(if (radar.loading) "레이더 불러오는 중" else (radar.error ?: "위치 대기 중"), style = IviType.body, color = c.ivi_text_secondary)
            return@IviCard
        }
        val now = System.currentTimeMillis() / 1000
        val m = cast.motion
        val lat = routeRain.firstOrNull()?.point?.lat ?: 37.0
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (m.confidence > 0) HeadingArrow(m.headingDeg(), 56.dp)
            Spacer(Modifier.width(IviSpace.m))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%.0f".format(m.speedKmh(lat, cast.latest.zoom)), style = IviType.number, color = c.ivi_text_primary)
                    Text(" km/h", style = IviType.body, color = c.ivi_text_secondary)
                }
                Text(
                    if (m.confidence > 0) "${compassKo(m.headingDeg())}쪽으로 이동 · 신뢰도 %.0f%%".format(m.confidence * 100) else "이동 추정 불가 (비구름 적음)",
                    style = IviType.caption, color = c.ivi_text_secondary,
                )
            }
        }
        Text("관측 ${hhmm(cast.latest.timeEpochSec)} (${(now - cast.latest.timeEpochSec) / 60}분 전)" +
            if (cast.field != null) " · 지역별 보정" else "", style = IviType.caption, color = c.ivi_text_label)
        radar.error?.let { Text(it, style = IviType.caption, color = c.ivi_status_caution) }
    }
}

@Composable
private fun FleetCard(fleet: FleetState, onToggleConsent: () -> Unit, onSeedDemo: () -> Unit) {
    val c = Ivi.colors
    IviCard(Modifier.fillMaxWidth(), label = "FLEET · 주변 Hermes 차량") {
        if (!fleet.configured) {
            Text("공유 서버가 설정되지 않았습니다", style = IviType.body, color = c.ivi_text_secondary)
            return@IviCard
        }
        val vehicles = fleet.reports.groupBy { it.vehicleId }.mapValues { (_, rs) -> rs.maxBy { it.observedAtEpochSec } }
        val wet = vehicles.values.count { it.level > RainLevel.NONE }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${vehicles.size}", style = IviType.number, color = c.ivi_text_primary)
            Text(" 대 · 비 관측 ", style = IviType.body, color = c.ivi_text_secondary)
            Text("$wet", style = IviType.number, color = if (wet > 0) c.ivi_status_caution else c.ivi_text_primary)
            Text(" 대", style = IviType.body, color = c.ivi_text_secondary)
        }
        Text("최근 20분 · 경로 주변 · 와이퍼 단계로 실제 비를 확인", style = IviType.caption, color = c.ivi_text_label)
        Row(horizontalArrangement = Arrangement.spacedBy(IviSpace.s)) {
            IviButton(if (fleet.consent) "내 관측 공유 중" else "내 관측 공유하기", onToggleConsent, primary = !fleet.consent)
            IviButton("시연", onSeedDemo, primary = false)
        }
        fleet.error?.let { Text(it, style = IviType.caption, color = c.ivi_status_caution) }
    }
}

@Composable
private fun WiperCard(vehicle: VehicleSnapshot, routeRain: List<RoutePointRain>) {
    val c = Ivi.colors
    IviCard(Modifier.fillMaxWidth(), label = "MY WIPER · 내 차 와이퍼") {
        val rain = vehicle.wiperSwitch.toRain()
        Text(rain.label, style = IviType.title, color = c.ivi_text_primary)
        val here = routeRain.firstOrNull()?.forecast?.let { (it as? PointForecast.Fleet)?.base ?: it }
        Text("와이퍼 ${vehicle.wiperSwitch?.name ?: "-"} · 레이더 판단 ${here?.rainLevel?.label ?: "-"}", style = IviType.caption, color = c.ivi_text_secondary)
    }
}

@Composable
private fun SourcesNote() {
    Text(
        "출처: 기상청 단기예보 · RainViewer 레이더 · Hermes 차량 와이퍼 관측",
        style = IviType.caption, color = Ivi.colors.ivi_text_label,
        modifier = Modifier.padding(horizontal = IviSpace.s),
    )
}

internal fun short(label: String, keepKind: Boolean = false): String {
    // "경유 1 대전 물류센터" → "대전 물류센터", "도착 부산신항" → "부산신항"
    val parts = label.split(' ')
    return when {
        keepKind -> label.removePrefix("경유 ").let { if (it.first().isDigit()) "경유 ${it}" else label }
        parts.first() == "경유" -> parts.drop(2).joinToString(" ")
        parts.first() == "도착" -> parts.drop(1).joinToString(" ")
        else -> label
    }
}

internal fun relative(sec: Long): String {
    val m = (sec / 60).coerceAtLeast(0)
    return if (m >= 60) "${m / 60}시간 ${m % 60}분 후" else "${m}분 후"
}

private val compassKo = listOf("북", "북동", "동", "남동", "남", "남서", "서", "북서")
internal fun compassKo(deg: Double) = compassKo[((deg + 22.5) / 45).toInt() % 8]
