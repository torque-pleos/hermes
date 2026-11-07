package com.zigae.hermes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zigae.hermes.radar.LegRain
import com.zigae.hermes.radar.RadarState
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePointRain
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviSpace
import com.zigae.hermes.ui.theme.IviType
import com.zigae.hermes.ui.theme.rainColor

/**
 * 경로: 왼쪽은 정차지를 선으로 이은 목록(줄 전체가 터치 대상), 오른쪽은 선택한 정차지 상세 한 면.
 * 좁은 창에서는 위아래로 쌓는다.
 */
@Composable
fun RouteScreen(
    routeRain: List<RoutePointRain>,
    legs: List<LegRain>,
    radar: RadarState,
    selected: Int,
    onSelect: (Int) -> Unit,
    onRequestTestRoute: () -> Unit,
) {
    val c = Ivi.colors
    val now = System.currentTimeMillis() / 1000
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 900.dp
        if (routeRain.size < 2) {
            Column(Modifier.padding(IviSpace.xl), verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                Text("경로", style = IviType.headline, color = c.ivi_text_primary)
                Text("내비게이션에 목적지가 없습니다", style = IviType.display, color = c.ivi_text_primary)
                IviButton("시연 경로 넣기: 대전 경유, 부산신항 도착", onRequestTestRoute, icon = HermesIcons.Navigation)
            }
            return@BoxWithConstraints
        }
        val sel = selected.coerceIn(0, routeRain.lastIndex)
        val list: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                Text("경로", style = IviType.headline, color = c.ivi_text_primary)
                Spacer(Modifier.height(IviSpace.l))
                routeRain.forEachIndexed { i, p ->
                    StopRow(i, p, now, legs.getOrNull(i - 1), i == sel, first = i == 0, last = i == routeRain.lastIndex) { onSelect(i) }
                }
            }
        }
        val detail: @Composable (Modifier) -> Unit = { m -> StopDetail(routeRain[sel], sel, legs.getOrNull(sel - 1), now, radar, m) }
        if (wide) {
            Row(Modifier.fillMaxSize().padding(horizontal = IviSpace.xl, vertical = IviSpace.l), horizontalArrangement = Arrangement.spacedBy(IviSpace.xl)) {
                list(Modifier.weight(0.42f).verticalScroll(rememberScrollState()))
                detail(Modifier.weight(0.58f).verticalScroll(rememberScrollState()))
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(IviSpace.l), verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                list(Modifier)
                detail(Modifier)
            }
        }
    }
}

/**
 * 정차지 한 줄: 왼쪽 세로선과 점(출발은 속이 찬 점), 이름·도착 시각, 오른쪽 도착 시 강수 아이콘.
 * 점 사이 세로선은 그 구간 최대 강수 색으로 칠해 가는 길의 비를 목록에서도 보이게 한다.
 */
@Composable
private fun StopRow(
    index: Int, p: RoutePointRain, now: Long, legBefore: LegRain?, selected: Boolean,
    first: Boolean, last: Boolean, onClick: () -> Unit,
) {
    val c = Ivi.colors
    val worstOnWay = legBefore?.worst?.forecast?.rainLevel
    val lineIn = if (legBefore != null) rainColor(worstOnWay).takeIf { (worstOnWay ?: RainLevel.NONE) > RainLevel.NONE } ?: c.ivi_border_card else c.ivi_border_card
    val nodeColor = if (selected) c.ivi_accent_primary else c.ivi_text_secondary
    Pressable(
        onClick, Modifier.fillMaxWidth().defaultMinSize(minHeight = 128.dp),
        color = if (selected) c.ivi_surface_card else Color.Transparent,
    ) {
        Row(Modifier.height(IntrinsicSize.Min).padding(end = IviSpace.l), verticalAlignment = Alignment.CenterVertically) {
            // 세로선 + 점
            Canvas(Modifier.width(72.dp).fillMaxHeight()) {
                val x = size.width / 2
                val y = size.height / 2
                val lw = 8.dp.toPx()
                if (!first) drawLine(lineIn, Offset(x, 0f), Offset(x, y), strokeWidth = lw)
                if (!last) drawLine(c.ivi_border_card, Offset(x, y), Offset(x, size.height), strokeWidth = lw)
                val r = 14.dp.toPx()
                if (first) drawCircle(nodeColor, r, Offset(x, y))
                else {
                    drawCircle(c.ivi_background_app, r, Offset(x, y))
                    drawCircle(nodeColor, r, Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
                }
            }
            Column(Modifier.weight(1f).padding(vertical = IviSpace.m)) {
                Text(if (index == 0) "현재 위치" else short(p.point.label), style = IviType.title, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (index == 0) "지금 출발" else "${hhmm(p.point.etaEpochSec)} 도착, ${relative(p.point.etaEpochSec - now)}",
                    style = IviType.body, color = c.ivi_text_secondary,
                )
                if (worstOnWay != null && worstOnWay > RainLevel.NONE) {
                    Text("가는 길에 ${worstOnWay.label}", style = IviType.bodyStrong, color = rainColor(worstOnWay))
                }
            }
            Spacer(Modifier.width(IviSpace.m))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RainGlyph(p.forecast.rainLevel, 52.dp)
                Text(arrivalText(p.forecast), style = IviType.caption, color = c.ivi_text_secondary)
            }
        }
    }
}

/** 선택한 정차지 상세: 한 면 안에 도착 시 강수 → 가는 길 막대 → 비구름 이동을 구분선으로 나눈다 */
@Composable
private fun StopDetail(p: RoutePointRain, index: Int, leg: LegRain?, now: Long, radar: RadarState, modifier: Modifier) {
    val c = Ivi.colors
    Surface(modifier, shape = RoundedCornerShape(IviSpace.cardRadius), color = c.ivi_surface_card) {
        Column(Modifier.padding(IviSpace.xl), verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
            Column {
                Text(if (index == 0) "현재 위치" else short(p.point.label), style = IviType.display, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (index == 0) "지금" else "${kind(p.point.label, index)}, ${hhmm(p.point.etaEpochSec)} 도착 (${relative(p.point.etaEpochSec - now)})",
                    style = IviType.title, color = c.ivi_text_secondary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RainGlyph(p.forecast.rainLevel, 88.dp)
                Spacer(Modifier.width(IviSpace.l))
                Column {
                    Text(
                        (if (index == 0) "지금 " else "도착할 때 ") + arrivalText(p.forecast),
                        style = IviType.headline, color = c.ivi_text_primary,
                    )
                    Text(sourceText(p.forecast), style = IviType.body, color = c.ivi_text_secondary, maxLines = 2)
                }
            }
            if (leg != null) {
                HDivider()
                Text("가는 길 ${hhmm(leg.from.etaEpochSec)}–${hhmm(leg.to.etaEpochSec)}", style = IviType.label, color = c.ivi_text_secondary)
                LegBars(leg)
                val w = leg.worst
                val l = w?.forecast?.rainLevel
                Text(
                    when {
                        w == null -> "예보 범위 밖 구간입니다"
                        l == null || l == RainLevel.NONE -> "가는 길에 비 소식 없음"
                        else -> "${hhmm(w.point.etaEpochSec)}쯤 ${l.label}, ${sourceText(w.forecast)}"
                    },
                    style = IviType.bodyStrong, color = c.ivi_text_primary, maxLines = 2,
                )
                if (leg.beyondHorizon > 0) Text("표본 ${leg.samples.size}곳 중 ${leg.beyondHorizon}곳은 예보 범위 밖", style = IviType.caption, color = c.ivi_text_secondary)
            }
            radar.nowcaster?.let { cast ->
                val m = cast.motion
                if (m.confidence > 0) {
                    HDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeadingArrow(m.headingDeg(), 56.dp)
                        Spacer(Modifier.width(IviSpace.l))
                        Column {
                            Text("비구름이 ${compassKo(m.headingDeg())}쪽으로 %.0fkm/h".format(m.speedKmh(p.point.lat, cast.latest.zoom)), style = IviType.title, color = c.ivi_text_primary)
                            Text(
                                "${hhmm(cast.latest.timeEpochSec)} 레이더 관측, 신뢰도 %.0f%%".format(m.confidence * 100) + if (m.confidence < 0.2) " (비구름이 적어 참고용)" else "",
                                style = IviType.caption, color = c.ivi_text_secondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 구간 표본별 강수 막대: 높이 = 강수 단계, 색 = 단계, 예보 없음은 낮은 회색 */
@Composable
private fun LegBars(leg: LegRain) {
    val samples = leg.samples
    val colors = samples.map { rainColor(it.forecast.rainLevel) }
    val levels = samples.map { it.forecast.rainLevel }
    val base = Ivi.colors.ivi_border_card
    Column {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val n = samples.size
            val gap = 8f
            val bw = (size.width - gap * (n - 1)) / n
            drawLine(base, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            levels.forEachIndexed { i, l ->
                val frac = when (l) {
                    null -> 0.08f; RainLevel.NONE -> 0.14f; RainLevel.LIGHT -> 0.35f
                    RainLevel.MODERATE -> 0.58f; RainLevel.HEAVY -> 0.8f; RainLevel.VERY_HEAVY -> 1f
                }
                val h = size.height * frac
                drawRoundRect(colors[i], Offset(i * (bw + gap), size.height - h), Size(bw, h), CornerRadius(10f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = IviSpace.xs)) {
            Text(hhmm(leg.from.etaEpochSec), style = IviType.caption, color = Ivi.colors.ivi_text_secondary, modifier = Modifier.weight(1f))
            Text(hhmm(leg.to.etaEpochSec), style = IviType.caption, color = Ivi.colors.ivi_text_secondary)
        }
    }
}
