package com.zigae.hermes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zigae.hermes.radar.LegRain
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviType
import com.zigae.hermes.ui.theme.rainColor

/**
 * 경로 강수 띠: 지금부터 도착까지의 시간축을 구간 표본별 강수 색으로 칠한다.
 * 정차지는 흰 눈금, 지금부터 60분(레이더·차량 관측 구간)은 점선 경계로 표시한다.
 */
@Composable
fun RainStrip(legs: List<LegRain>, nowEpochSec: Long) {
    val samples = legs.flatMap { it.samples }
    if (samples.size < 2) return
    val start = nowEpochSec
    val end = samples.last().point.etaEpochSec.coerceAtLeast(start + 60)
    val span = (end - start).toFloat()
    val colors = samples.map { rainColor(it.forecast.rainLevel) }
    val unknown = samples.map { it.forecast.rainLevel == null }
    val stripeBg = Ivi.colors.ivi_surface_card
    val tick = Ivi.colors.ivi_text_primary
    val radarEdge = Ivi.colors.ivi_text_label
    val stops = legs.map { it.to.etaEpochSec }

    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(76.dp)) {
            val w = size.width
            val h = size.height
            fun x(t: Long) = ((t - start).coerceIn(0, end - start) / span) * w
            samples.forEachIndexed { i, s ->
                val x0 = x(s.point.etaEpochSec)
                val x1 = if (i + 1 < samples.size) x(samples[i + 1].point.etaEpochSec) else w
                if (x1 <= x0) return@forEachIndexed
                if (unknown[i]) {
                    // 예보 없음: 빗금
                    drawRect(stripeBg, Offset(x0, 0f), Size(x1 - x0 + 1f, h))
                    clipRect(x0, 0f, x1 + 1f, h) {
                        var sx = x0 - h
                        while (sx < x1) {
                            drawLine(colors[i], Offset(sx, h), Offset(sx + h, 0f), strokeWidth = 4f)
                            sx += 14f
                        }
                    }
                } else {
                    drawRect(colors[i], Offset(x0, 0f), Size(x1 - x0 + 1f, h))
                }
            }
            // 둥근 모서리 효과: 양 끝을 배경과 같은 투명 처리 대신 테두리로 마감
            drawRoundRect(tick.copy(alpha = 0.25f), size = Size(w, h), cornerRadius = CornerRadius(18f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            stops.forEach { t -> drawRect(tick, Offset(x(t) - 2f, -6f), Size(4f, h + 12f)) }
            val radar = x(start + 60 * 60)
            if (radar < w) drawLine(
                radarEdge, Offset(radar, 0f), Offset(radar, h), strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }
        // 정차지 시각 라벨
        BoxWithConstraints(Modifier.fillMaxWidth().height(34.dp).padding(top = 6.dp)) {
            val wPx = with(LocalDensity.current) { maxWidth.toPx() }
            legs.forEach { leg ->
                val f = ((leg.to.etaEpochSec - start).toFloat() / span).coerceIn(0f, 1f)
                val offsetDp = with(LocalDensity.current) { (f * wPx).toDp() }
                Box(Modifier.offset(x = (offsetDp - 56.dp).coerceAtLeast(0.dp).coerceAtMost(maxWidth - 112.dp))) {
                    Text(hhmm(leg.to.etaEpochSec), style = IviType.caption, color = Ivi.colors.ivi_text_secondary)
                }
            }
            Text("지금", style = IviType.caption, color = Ivi.colors.ivi_text_secondary)
        }
    }
}
