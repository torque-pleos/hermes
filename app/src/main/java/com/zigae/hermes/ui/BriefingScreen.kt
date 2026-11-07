package com.zigae.hermes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
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
 * 브리핑: 결론 한 문장(가장 큰 글자) → 경로 강수 띠(정차지 이름·도착 시각) → 사실 세 칸.
 * 결론과 띠가 화면의 중심이고, 나머지는 한 면 안에 나란히 둔다.
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
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = IviSpace.xl, vertical = IviSpace.l),
            verticalArrangement = Arrangement.spacedBy(IviSpace.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("운행 브리핑", style = IviType.headline, color = c.ivi_text_primary, modifier = Modifier.weight(1f))
                Text("${hhmm(now)} 기준", style = IviType.body, color = c.ivi_text_secondary)
            }

            if (legs.isEmpty() && !loading) {
                EmptyRoute(routeRain, onRequestTestRoute)
                return@Column
            }
            Verdict(legs, loading) { idx -> onOpenStop(idx) }

            if (legs.isNotEmpty()) Pressable({ onOpenStop(1) }, Modifier.fillMaxWidth(), color = c.ivi_surface_card) {
                Column(Modifier.padding(start = IviSpace.l, end = IviSpace.l, top = IviSpace.m, bottom = IviSpace.l), verticalArrangement = Arrangement.spacedBy(IviSpace.s)) {
                    RainStrip(legs, now)
                    Spacer(Modifier.height(IviSpace.xs))
                    Legend()
                }
            }

            val facts: List<@Composable (Modifier) -> Unit> = listOf(
                { m -> NextStopFact(legs, now, m) { onOpenStop(1) } },
                { m -> RadarFact(radar, routeRain, m) { onOpenStop(1) } },
                { m -> FleetFact(fleet, m, onOpenFleet) },
            )
            Surface(shape = RoundedCornerShape(IviSpace.cardRadius), color = c.ivi_surface_card, modifier = Modifier.fillMaxWidth()) {
                if (wide) {
                    Row(Modifier.height(IntrinsicSize.Min).padding(vertical = IviSpace.s)) {
                        facts.forEachIndexed { i, f ->
                            if (i > 0) Box(Modifier.padding(vertical = IviSpace.m)) { VDivider() }
                            f(Modifier.weight(1f))
                        }
                    }
                } else {
                    Column { facts.forEach { it(Modifier.fillMaxWidth()) } }
                }
            }
            Text(
                "자료: 기상청 단기예보, RainViewer 레이더, Hermes 차량 와이퍼",
                style = IviType.caption, color = c.ivi_text_label,
            )
        }
    }
}

@Composable
private fun EmptyRoute(routeRain: List<RoutePointRain>, onRequestTestRoute: () -> Unit) {
    val c = Ivi.colors
    val here = routeRain.firstOrNull()?.forecast
    Column(Modifier.padding(top = IviSpace.l), verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RainGlyph(here?.rainLevel, 88.dp)
            Spacer(Modifier.width(IviSpace.l))
            Column {
                Text("목적지가 없습니다", style = IviType.display, color = c.ivi_text_primary)
                Text("현재 위치 ${here?.let { arrivalText(it) } ?: "확인 중"}", style = IviType.title, color = c.ivi_text_secondary)
            }
        }
        Text("내비게이션에 목적지와 경유지를 넣으면 정차지마다 도착할 때의 비를 알려드립니다.", style = IviType.body, color = c.ivi_text_secondary)
        IviButton("시연 경로 넣기: 대전 경유, 부산신항 도착", onRequestTestRoute, icon = HermesIcons.Navigation)
    }
}

/** 결론: 가장 강한 비의 시각·구간·근거. 비가 있으면 강수 색 면, 없으면 면 없이 글자만. 누르면 그 구간 정차지 상세 */
@Composable
private fun Verdict(legs: List<LegRain>, loading: Boolean, onOpen: (Int) -> Unit) {
    val c = Ivi.colors
    val worst = worstOf(legs)
    val level = worst?.second?.forecast?.rainLevel
    val rainy = !loading && level != null
    val dest = legs.lastOrNull()?.to
    Pressable(
        { onOpen((worst?.first ?: 0) + 1) }, Modifier.fillMaxWidth(),
        color = if (rainy) rainColor(level).copy(alpha = 0.16f) else Color.Transparent,
    ) {
        Row(Modifier.padding(horizontal = if (rainy) IviSpace.l else 0.dp, vertical = IviSpace.l), verticalAlignment = Alignment.CenterVertically) {
            RainGlyph(if (loading) null else (level ?: RainLevel.NONE), 120.dp)
            Spacer(Modifier.width(IviSpace.xl))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(IviSpace.xs)) {
                Text(
                    when {
                        loading -> "강수 예보를 불러오는 중"
                        worst == null -> "가는 길에 비 소식 없음"
                        else -> "${hhmm(worst.second.point.etaEpochSec)}쯤 ${level!!.label}"
                    },
                    style = IviType.hero, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val leg = worst?.let { legs[it.first] }
                Text(
                    when {
                        leg != null -> "${short(leg.from.label)}에서 ${short(leg.to.label)} 가는 길"
                        loading -> "레이더, 기상청, 주변 차량을 확인하고 있습니다"
                        dest != null -> "${short(dest.label)} ${hhmm(dest.etaEpochSec)} 도착까지 전 구간"
                        else -> "전 구간 확인"
                    },
                    style = IviType.title, color = c.ivi_text_secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                worst?.let { Row(Modifier.padding(top = IviSpace.s), horizontalArrangement = Arrangement.spacedBy(IviSpace.s)) { SourceChips(it.second.forecast) } }
            }
            Chevron()
        }
    }
}

@Composable
internal fun SourceChips(f: PointForecast) {
    val c = Ivi.colors
    when (f) {
        is PointForecast.Fleet -> {
            if (f.evidence.consensus != null) Chip("주변 차량 ${f.evidence.wetVehicles}대가 비 관측", c.ivi_status_caution, filled = true)
            SourceChips(f.base)
        }
        is PointForecast.Rain -> Chip("레이더 ${f.leadMinutes}분 예측", c.ivi_accent_primary)
        is PointForecast.Kma -> Chip("기상청 예보" + (f.pop?.let { ", 강수확률 $it%" } ?: ""), c.ivi_accent_primary)
        else -> {}
    }
}

@Composable
private fun NextStopFact(legs: List<LegRain>, now: Long, modifier: Modifier, onClick: () -> Unit) {
    val next = legs.firstOrNull() ?: return
    Fact(
        label = "다음 정차",
        value = "${hhmm(next.to.etaEpochSec)} 도착",
        detail = "${short(next.to.label)}, ${relative(next.to.etaEpochSec - now)}",
        onClick = onClick, modifier = modifier,
        leading = { RainGlyph(next.worst?.forecast?.rainLevel ?: RainLevel.NONE, 44.dp) },
    )
}

/** 이동 벡터 신뢰도가 이보다 낮으면 속도를 크게 내세우지 않는다 */
private const val MOTION_CONFIDENT = 0.2

@Composable
private fun RadarFact(radar: RadarState, routeRain: List<RoutePointRain>, modifier: Modifier, onClick: () -> Unit) {
    val cast = radar.nowcaster
    val lat = routeRain.firstOrNull()?.point?.lat ?: 37.0
    val m = cast?.motion?.takeIf { it.confidence > 0 }
    Fact(
        label = "비구름 이동",
        value = if (cast == null) "확인 중" else if (m == null) "추정 불가" else "${compassKo(m.headingDeg())}쪽 %.0fkm/h".format(m.speedKmh(lat, cast.latest.zoom)),
        detail = when {
            cast == null -> if (radar.loading) "레이더 불러오는 중" else (radar.error ?: "위치 대기 중")
            m == null -> "비구름이 적어 이동을 알 수 없음"
            m.confidence < MOTION_CONFIDENT -> "비구름이 적어 참고용, ${hhmm(cast.latest.timeEpochSec)} 관측"
            else -> "${hhmm(cast.latest.timeEpochSec)} 레이더 관측"
        },
        onClick = onClick, modifier = modifier,
        leading = m?.let { { HeadingArrow(it.headingDeg(), 44.dp) } },
    )
}

@Composable
private fun FleetFact(fleet: FleetState, modifier: Modifier, onClick: () -> Unit) {
    val c = Ivi.colors
    val vehicles = fleet.reports.groupBy { it.vehicleId }.mapValues { (_, rs) -> rs.maxBy { it.observedAtEpochSec } }
    val wet = vehicles.values.count { it.level > RainLevel.NONE }
    Fact(
        label = "주변 Hermes 차량",
        value = when {
            !fleet.configured -> "꺼짐"
            wet > 0 -> "${wet}대가 비 관측"
            else -> "비 관측 없음"
        },
        detail = if (!fleet.configured) "공유 서버가 설정되지 않음" else "최근 20분 ${vehicles.size}대, " + if (fleet.consent) "내 관측도 공유 중" else "내 관측은 공유 안 함",
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            val ink = Ivi.colors.ivi_text_primary.copy(alpha = 0.55f)
            androidx.compose.foundation.Canvas(Modifier.size(width = 6.dp, height = 28.dp)) {
                drawLine(ink, androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width / 2, size.height), strokeWidth = 3f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
            Text(" 점선 앞 1시간은 레이더 예측", style = IviType.caption, color = Ivi.colors.ivi_text_secondary)
        }
    }
}
