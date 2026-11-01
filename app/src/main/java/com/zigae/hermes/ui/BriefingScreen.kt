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
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zigae.hermes.fleet.FleetState
import com.zigae.hermes.radar.LegRain
import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.RadarState
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePointRain
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviSpace
import com.zigae.hermes.ui.theme.IviType
import com.zigae.hermes.ui.theme.rainColor
import com.zigae.hermes.weather.KmaState

/** 가장 강한 비가 있는 구간 표본 (같은 강도면 빠른 시각) */
internal fun worstOf(legs: List<LegRain>): Pair<Int, RoutePointRain>? =
    legs.mapIndexedNotNull { i, leg -> leg.worst?.let { i to it } }
        .filter { (it.second.forecast.rainLevel ?: RainLevel.NONE) > RainLevel.NONE }
        .maxWithOrNull(compareBy<Pair<Int, RoutePointRain>> { it.second.forecast.rainLevel }.thenByDescending { it.second.point.etaEpochSec })

/**
 * 브리핑: 큰 결론 카드 → 큰 숫자 타일 3개 → 경로 강수 띠. 모든 카드는 눌러서 자세히 볼 수 있다.
 */
@Composable
fun BriefingScreen(
    routeRain: List<RoutePointRain>,
    legs: List<LegRain>,
    radar: RadarState,
    kma: KmaState,
    fleet: FleetState,
    onOpenStop: (Int) -> Unit,
    onOpenFleet: () -> Unit,
    onRequestTestRoute: () -> Unit,
) {
    val c = Ivi.colors
    val now = System.currentTimeMillis() / 1000
    val loading = radar.nowcaster == null && kma.updatedAtEpochSec == 0L
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 900.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(IviSpace.xl),
            verticalArrangement = Arrangement.spacedBy(IviSpace.l),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("HERMES · 화물 운행 강수 코파일럿", style = IviType.label, color = c.ivi_text_label)
                    Text("운행 브리핑", style = IviType.display, color = c.ivi_text_primary)
                }
                Text("${hhmm(now)} 기준", style = IviType.body, color = c.ivi_text_secondary)
            }

            if (legs.isEmpty() && !loading) {
                EmptyRoute(routeRain, onRequestTestRoute)
                return@Column
            }
            Verdict(legs, loading) { idx -> onOpenStop(idx) }

            val tiles: List<@Composable (Modifier) -> Unit> = listOf(
                { m -> NextStopTile(legs, now, m) { onOpenStop(1) } },
                { m -> RadarTile(radar, routeRain, m) { onOpenStop(1) } },
                { m -> FleetTile(fleet, m, onOpenFleet) },
            )
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(IviSpace.l)) { tiles.forEach { it(Modifier.weight(1f)) } }
            } else {
                tiles.forEach { it(Modifier.fillMaxWidth()) }
            }

            Pressable({ onOpenStop(1) }, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(IviSpace.l), verticalArrangement = Arrangement.spacedBy(IviSpace.m)) {
                    SectionLabel("ROUTE RAIN · 지금부터 도착까지")
                    RainStrip(legs, now)
                    Legend()
                }
            }
            Text(
                "출처: 기상청 단기예보 · RainViewer 레이더 · Hermes 차량 와이퍼 관측",
                style = IviType.caption, color = c.ivi_text_label,
            )
        }
    }
}

@Composable
private fun EmptyRoute(routeRain: List<RoutePointRain>, onRequestTestRoute: () -> Unit) {
    val c = Ivi.colors
    IviCard(Modifier.fillMaxWidth()) {
        val here = routeRain.firstOrNull()?.forecast
        RainBadge(here?.rainLevel, "현재 위치 ${here?.let { arrivalText(it) } ?: "확인 중"}", 56.dp)
        Text("내비게이션에 목적지를 설정하면 경유지·도착지 강수를 알려드립니다.", style = IviType.title, color = c.ivi_text_secondary)
        IviButton("시연 경로 설정 (대전 경유 → 부산신항)", onRequestTestRoute, icon = Icons.Rounded.Navigation)
    }
}

/** 큰 결론 카드: 가장 강한 비의 시각·구간·근거. 누르면 그 구간의 정차지 상세 */
@Composable
private fun Verdict(legs: List<LegRain>, loading: Boolean, onOpen: (Int) -> Unit) {
    val c = Ivi.colors
    val worst = worstOf(legs)
    val level = worst?.second?.forecast?.rainLevel
    val tone = when {
        loading -> c.ivi_text_label
        level == null -> c.ivi_status_safe
        else -> rainColor(level)
    }
    Pressable(
        { onOpen((worst?.first ?: 0) + 1) }, Modifier.fillMaxWidth(),
        color = tone.copy(alpha = 0.12f), border = tone.copy(alpha = 0.8f), borderWidth = 2.dp,
    ) {
        Row(Modifier.padding(IviSpace.xl), verticalAlignment = Alignment.CenterVertically) {
            RainGlyph(if (loading) null else (level ?: RainLevel.NONE), 112.dp)
            Spacer(Modifier.width(IviSpace.xl))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(IviSpace.xs)) {
                Text(
                    when {
                        loading -> "강수 예보 불러오는 중"
                        worst == null -> "가는 길 비 예보 없음"
                        else -> "${hhmm(worst.second.point.etaEpochSec)} 부근 ${level!!.label}"
                    },
                    style = IviType.hero, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val leg = worst?.let { legs[it.first] }
                Text(
                    when {
                        leg != null -> "${short(leg.from.label)} → ${short(leg.to.label)} 구간"
                        !loading -> "경로 전 구간 확인 완료"
                        else -> "레이더 · 기상청 · 주변 차량 확인 중"
                    },
                    style = IviType.title, color = c.ivi_text_secondary, maxLines = 1,
                )
                worst?.let { Row(Modifier.padding(top = IviSpace.s), horizontalArrangement = Arrangement.spacedBy(IviSpace.s)) { SourceChips(it.second.forecast) } }
            }
        }
    }
}

@Composable
internal fun SourceChips(f: PointForecast) {
    val c = Ivi.colors
    when (f) {
        is PointForecast.Fleet -> {
            if (f.evidence.consensus != null) Chip("주변 차량 ${f.evidence.wetVehicles}대 관측", c.ivi_status_caution, filled = true)
            SourceChips(f.base)
        }
        is PointForecast.Rain -> Chip("레이더 ${f.leadMinutes}분 예측", c.ivi_accent_primary)
        is PointForecast.Kma -> Chip("기상청" + (f.pop?.let { " · 확률 $it%" } ?: ""), c.ivi_accent_primary)
        else -> {}
    }
}

@Composable
private fun NextStopTile(legs: List<LegRain>, now: Long, modifier: Modifier, onClick: () -> Unit) {
    val next = legs.firstOrNull() ?: return
    StatTile(
        label = "다음 정차",
        value = hhmm(next.to.etaEpochSec),
        unit = "도착",
        detail = "${short(next.to.label)} · ${relative(next.to.etaEpochSec - now)}",
        onClick = onClick, modifier = modifier,
        leading = { RainGlyph(next.worst?.forecast?.rainLevel ?: RainLevel.NONE, 52.dp) },
    )
}

@Composable
private fun RadarTile(radar: RadarState, routeRain: List<RoutePointRain>, modifier: Modifier, onClick: () -> Unit) {
    val cast = radar.nowcaster
    val lat = routeRain.firstOrNull()?.point?.lat ?: 37.0
    val m = cast?.motion
    StatTile(
        label = "비구름 이동",
        value = if (cast == null || m == null || m.confidence <= 0) "-" else "%.0f".format(m.speedKmh(lat, cast.latest.zoom)),
        unit = "km/h",
        detail = when {
            cast == null -> if (radar.loading) "레이더 불러오는 중" else (radar.error ?: "위치 대기 중")
            m == null || m.confidence <= 0 -> "비구름이 적어 이동 추정 불가"
            else -> "${compassKo(m.headingDeg())}쪽으로 이동 · 관측 ${hhmm(cast.latest.timeEpochSec)}"
        },
        onClick = onClick, modifier = modifier,
        leading = if (m != null && m.confidence > 0) ({ HeadingArrow(m.headingDeg(), 52.dp) }) else null,
    )
}

@Composable
private fun FleetTile(fleet: FleetState, modifier: Modifier, onClick: () -> Unit) {
    val c = Ivi.colors
    val vehicles = fleet.reports.groupBy { it.vehicleId }.mapValues { (_, rs) -> rs.maxBy { it.observedAtEpochSec } }
    val wet = vehicles.values.count { it.level > RainLevel.NONE }
    StatTile(
        label = "주변 Hermes 차량",
        value = if (fleet.configured) "$wet" else "-",
        unit = "대 비 관측",
        detail = if (!fleet.configured) "공유 서버 미설정" else "최근 20분 ${vehicles.size}대 · " + if (fleet.consent) "내 관측 공유 중" else "공유 꺼짐",
        onClick = onClick, modifier = modifier,
        valueColor = if (wet > 0) c.ivi_status_caution else c.ivi_text_primary,
    )
}

@Composable
internal fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(IviSpace.l), verticalAlignment = Alignment.CenterVertically) {
        listOf(RainLevel.NONE, RainLevel.LIGHT, RainLevel.MODERATE, RainLevel.HEAVY, RainLevel.VERY_HEAVY, null).forEach { l ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RainGlyph(l, 28.dp)
                Text(" ${l?.label ?: "예보 없음"}", style = IviType.caption, color = Ivi.colors.ivi_text_secondary)
            }
        }
    }
}
