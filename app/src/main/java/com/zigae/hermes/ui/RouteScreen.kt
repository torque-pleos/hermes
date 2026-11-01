package com.zigae.hermes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
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
 * 경로: 왼쪽 정차지 카드 목록(큰 터치 대상) · 오른쪽 선택한 정차지 상세(가는 길 시간대별 강수 막대, 근거).
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
                Text("경로", style = IviType.display, color = c.ivi_text_primary)
                Text("내비게이션에 목적지가 없습니다.", style = IviType.title, color = c.ivi_text_secondary)
                IviButton("시연 경로 설정 (대전 경유 → 부산신항)", onRequestTestRoute, icon = Icons.Rounded.Navigation)
            }
            return@BoxWithConstraints
        }
        val sel = selected.coerceIn(0, routeRain.lastIndex)
        val list: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(IviSpace.m)) {
                Text("경로", style = IviType.display, color = c.ivi_text_primary)
                Text("정차지를 눌러 가는 길 강수를 확인하세요", style = IviType.body, color = c.ivi_text_secondary)
                Spacer(Modifier.height(IviSpace.s))
                routeRain.forEachIndexed { i, p -> StopCard(i, p, now, legs.getOrNull(i - 1), i == sel) { onSelect(i) } }
            }
        }
        val detail: @Composable (Modifier) -> Unit = { m -> StopDetail(routeRain[sel], sel, legs.getOrNull(sel - 1), now, radar, m) }
        if (wide) {
            Row(Modifier.fillMaxSize().padding(IviSpace.xl), horizontalArrangement = Arrangement.spacedBy(IviSpace.xl)) {
                list(Modifier.weight(0.45f).verticalScroll(rememberScrollState()))
                detail(Modifier.weight(0.55f).verticalScroll(rememberScrollState()))
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(IviSpace.l), verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
                list(Modifier)
                detail(Modifier)
            }
        }
    }
}

/** 정차지 카드: 번호 · 이름 · 도착 시각 · 도착 시 강수 아이콘 (최소 높이 120dp) */
@Composable
private fun StopCard(index: Int, p: RoutePointRain, now: Long, legBefore: LegRain?, selected: Boolean, onClick: () -> Unit) {
    val c = Ivi.colors
    val worstOnWay = legBefore?.worst?.forecast?.rainLevel
    Pressable(
        onClick, Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp),
        color = if (selected) c.ivi_accent_primary.copy(alpha = 0.12f) else c.ivi_surface_card,
        border = if (selected) c.ivi_accent_primary else c.ivi_border_card,
        borderWidth = if (selected) 2.dp else 1.dp,
    ) {
        Row(Modifier.padding(IviSpace.l), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(if (index == 0) c.ivi_accent_primary else c.ivi_surface_card_raised),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (index == 0) "●" else "$index", style = IviType.title, color = if (index == 0) c.ivi_accent_on_primary else c.ivi_text_primary)
            }
            Spacer(Modifier.width(IviSpace.l))
            Column(Modifier.weight(1f)) {
                Text(kind(p.point.label, index), style = IviType.label, color = c.ivi_text_label)
                Text(if (index == 0) "현재 위치" else short(p.point.label), style = IviType.title, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (index == 0) "지금" else "${hhmm(p.point.etaEpochSec)} 도착 · ${relative(p.point.etaEpochSec - now)}",
                    style = IviType.body, color = c.ivi_text_secondary,
                )
                if (worstOnWay != null && worstOnWay > RainLevel.NONE) {
                    Text("가는 길 ${worstOnWay.label}", style = IviType.caption, color = rainColor(worstOnWay))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RainGlyph(p.forecast.rainLevel, 56.dp)
                Text(arrivalText(p.forecast), style = IviType.caption, color = c.ivi_text_secondary)
            }
        }
    }
}

@Composable
private fun StopDetail(p: RoutePointRain, index: Int, leg: LegRain?, now: Long, radar: RadarState, modifier: Modifier) {
    val c = Ivi.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(IviSpace.l)) {
        IviCard(Modifier.fillMaxWidth(), label = if (index == 0) "NOW · 현재 위치" else "ARRIVAL · ${kind(p.point.label, index)}") {
            Text(if (index == 0) "현재 위치" else short(p.point.label), style = IviType.display, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (index == 0) "지금 이 위치" else "${hhmm(p.point.etaEpochSec)} 도착 · ${relative(p.point.etaEpochSec - now)}",
                style = IviType.title, color = c.ivi_text_secondary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RainGlyph(p.forecast.rainLevel, 88.dp)
                Spacer(Modifier.width(IviSpace.l))
                Column {
                    Text(if (index == 0) "지금 강수" else "도착할 때 강수", style = IviType.label, color = c.ivi_text_label)
                    Text(arrivalText(p.forecast), style = IviType.headline, color = c.ivi_text_primary)
                    Text(sourceText(p.forecast), style = IviType.caption, color = c.ivi_text_secondary, maxLines = 2)
                }
            }
        }
        if (leg != null) {
            IviCard(Modifier.fillMaxWidth(), label = "ON THE WAY · 가는 길 ${hhmm(leg.from.etaEpochSec)}~${hhmm(leg.to.etaEpochSec)}") {
                LegBars(leg)
                val w = leg.worst
                val l = w?.forecast?.rainLevel
                Text(
                    when {
                        w == null -> "예보 범위 밖 구간입니다"
                        l == null || l == RainLevel.NONE -> "가는 길 비 예보 없음"
                        else -> "${hhmm(w.point.etaEpochSec)} 부근 ${l.label} · ${sourceText(w.forecast)}"
                    },
                    style = IviType.bodyStrong, color = c.ivi_text_primary, maxLines = 2,
                )
                if (leg.beyondHorizon > 0) Text("표본 ${leg.samples.size}개 중 ${leg.beyondHorizon}개는 예보 범위 밖", style = IviType.caption, color = c.ivi_text_label)
                Text("직선 근사 · ${(com.zigae.hermes.radar.RouteRain.SAMPLE_SPACING_M / 1000).toInt()}km 간격 표본", style = IviType.caption, color = c.ivi_text_label)
            }
        }
        radar.nowcaster?.let { cast ->
            val m = cast.motion
            if (m.confidence > 0) IviCard(Modifier.fillMaxWidth(), label = "RADAR · 비구름 이동") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeadingArrow(m.headingDeg(), 64.dp)
                    Spacer(Modifier.width(IviSpace.l))
                    Column {
                        Text("${compassKo(m.headingDeg())}쪽 %.0fkm/h".format(m.speedKmh(p.point.lat, cast.latest.zoom)), style = IviType.headline, color = c.ivi_text_primary)
                        Text("관측 ${hhmm(cast.latest.timeEpochSec)} · 신뢰도 %.0f%%".format(m.confidence * 100) + if (cast.field != null) " · 지역별 보정" else "", style = IviType.caption, color = c.ivi_text_secondary)
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
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
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
