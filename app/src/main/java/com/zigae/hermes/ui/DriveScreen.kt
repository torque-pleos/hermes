package com.zigae.hermes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zigae.hermes.radar.LegRain
import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePointRain
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviSpace
import com.zigae.hermes.ui.theme.IviType
import com.zigae.hermes.ui.theme.rainColor

/**
 * 주행 중 화면 (DO). 1~2초 안에 읽히도록 3줄 이내, 입력·스크롤·애니메이션 없음, 큰 글자.
 * - 다음 정차지와 도착 시각
 * - 다음 구간 최악 강수(큰 색 블록)와 시각, 근거 한 줄
 * - 그다음 구간 한 줄
 */
@Composable
fun DriveScreen(routeRain: List<RoutePointRain>, legs: List<LegRain>, loading: Boolean) {
    val c = Ivi.colors
    Column(
        Modifier.fillMaxSize().background(c.ivi_background_app).padding(IviSpace.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            Text("강수 예보 불러오는 중", style = IviType.headline, color = c.ivi_text_secondary)
            return@Column
        }
        val next = legs.firstOrNull()
        if (next == null) {
            val here = routeRain.firstOrNull()?.forecast
            Hero(here?.rainLevel, "현재 위치 ${here?.rainLevel?.let { if (it == RainLevel.NONE) "비 없음" else it.label } ?: "정보 없음"}", null)
            Spacer(Modifier.height(IviSpace.l))
            Text("목적지를 설정하면 구간 강수를 알려드립니다", style = IviType.title, color = c.ivi_text_secondary)
            return@Column
        }
        Text("다음 정차", style = IviType.label, color = c.ivi_text_label)
        Text(
            "${short(next.to.label)} · ${hhmm(next.to.etaEpochSec)} 도착",
            style = IviType.display, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(IviSpace.xl))
        val worst = next.worst
        val level = worst?.forecast?.rainLevel
        Hero(
            level,
            if (worst == null || level == null || level == RainLevel.NONE) "가는 길 비 예보 없음"
            else "${hhmm(worst.point.etaEpochSec)} 부근 ${level.label}",
            when {
                (worst?.forecast as? PointForecast.Fleet)?.evidence?.consensus != null ->
                    "주변 차량 ${(worst.forecast as PointForecast.Fleet).evidence.wetVehicles}대가 비 관측"
                worst == null && next.beyondHorizon > 0 -> "예보 범위 밖 구간 있음"
                else -> null
            },
        )
        legs.getOrNull(1)?.let { after ->
            Spacer(Modifier.height(IviSpace.xl))
            val w = after.worst
            val l = w?.forecast?.rainLevel
            Row(verticalAlignment = Alignment.CenterVertically) {
                RainGlyph(l, 44.dp)
                Spacer(Modifier.width(IviSpace.m))
                Text(
                    "이후 ${short(after.to.label)} ${hhmm(after.to.etaEpochSec)} · " +
                        when {
                            w == null || l == null -> "예보 범위 밖"
                            l == RainLevel.NONE -> "비 예보 없음"
                            else -> "${hhmm(w.point.etaEpochSec)} 부근 ${l.label}"
                        },
                    style = IviType.title, color = c.ivi_text_secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Hero(level: RainLevel?, text: String, sub: String?) {
    val c = Ivi.colors
    val color = rainColor(level)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(color.copy(alpha = 0.14f))
            .border(2.dp, color, RoundedCornerShape(28.dp)).padding(IviSpace.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RainGlyph(level ?: RainLevel.NONE, 112.dp)
        Spacer(Modifier.width(IviSpace.l))
        Column {
            Text(text, style = IviType.display, color = c.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            sub?.let { Text(it, style = IviType.title, color = c.ivi_text_secondary, maxLines = 1) }
        }
    }
}
