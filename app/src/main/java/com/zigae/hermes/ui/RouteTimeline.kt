package com.zigae.hermes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zigae.hermes.radar.LegRain
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviType
import com.zigae.hermes.ui.theme.rainColor

/**
 * 경로 강수 띠: 지금부터 도착까지를 가로 시간축으로 펴고 구간 표본별 강수 색으로 칠한다.
 * 위에는 정차지 이름, 아래에는 도착 시각을 정차지 눈금에 맞춰 단다.
 * 지금부터 60분(레이더·차량 관측으로 예측하는 구간)은 점선으로 경계를 긋는다.
 */
@Composable
fun RainStrip(legs: List<LegRain>, nowEpochSec: Long, stripHeight: Dp = 96.dp) {
    val samples = legs.flatMap { it.samples }
    if (samples.size < 2) return
    val start = nowEpochSec
    val end = samples.last().point.etaEpochSec.coerceAtLeast(start + 60)
    val span = (end - start).toFloat()
    fun frac(t: Long) = ((t - start).toFloat() / span).coerceIn(0f, 1f)

    val colors = samples.map { rainColor(it.forecast.rainLevel) }
    val unknown = samples.map { it.forecast.rainLevel == null }
    val stripeBg = Ivi.colors.ivi_surface_card_raised
    val tick = Ivi.colors.ivi_text_primary
    val radarEdge = Ivi.colors.ivi_text_primary.copy(alpha = 0.55f)
    val stops = legs.map { it.to }

    Column(Modifier.fillMaxWidth()) {
        // 정차지 이름 (눈금 위)
        StopLabels(stops.map { frac(it.etaEpochSec) to short(it.label) }, IviType.bodyStrong, Ivi.colors.ivi_text_primary)
        Canvas(Modifier.fillMaxWidth().height(stripHeight)) {
            val w = size.width
            val h = size.height
            val r = 16.dp.toPx()
            val outline = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h, CornerRadius(r))) }
            clipPath(outline) {
                samples.forEachIndexed { i, s ->
                    val x0 = frac(s.point.etaEpochSec) * w
                    val x1 = if (i + 1 < samples.size) frac(samples[i + 1].point.etaEpochSec) * w else w
                    if (x1 <= x0) return@forEachIndexed
                    if (unknown[i]) {
                        // 예보 없음: 빗금
                        drawRect(stripeBg, Offset(x0, 0f), Size(x1 - x0 + 1f, h))
                        var sx = x0 - h
                        while (sx < x1) {
                            drawLine(colors[i], Offset(sx, h), Offset(sx + h, 0f), strokeWidth = 4f)
                            sx += 14f
                        }
                    } else {
                        drawRect(colors[i], Offset(x0, 0f), Size(x1 - x0 + 1f, h))
                    }
                }
            }
            val radar = frac(start + 60 * 60) * w
            if (radar < w) drawLine(
                radarEdge, Offset(radar, 0f), Offset(radar, h), strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            // 정차지 눈금: 띠 위아래로 조금 튀어나오게
            stops.forEach { s ->
                val x = (frac(s.etaEpochSec) * w).coerceIn(3f, w - 3f)
                drawRect(tick, Offset(x - 3f, -8f), Size(6f, h + 16f))
            }
        }
        // 도착 시각 (눈금 아래) + 왼쪽 끝 "지금"
        StopLabels(listOf(0f to "지금") + stops.map { frac(it.etaEpochSec) to hhmm(it.etaEpochSec) }, IviType.caption, Ivi.colors.ivi_text_secondary)
    }
}

/** 띠 비율 위치(0~1)에 맞춰 글자를 단다. 양 끝에서는 안쪽으로 붙인다 */
@Composable
private fun StopLabels(items: List<Pair<Float, String>>, style: androidx.compose.ui.text.TextStyle, color: androidx.compose.ui.graphics.Color) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(style.lineHeight.value.dp + 8.dp)) {
        val boxW = 260.dp
        items.forEach { (f, text) ->
            val center = maxWidth * f
            val (x, align) = when {
                f < 0.08f -> 0.dp to TextAlign.Start
                f > 0.92f -> (maxWidth - boxW) to TextAlign.End
                else -> (center - boxW / 2) to TextAlign.Center
            }
            Box(Modifier.offset(x = x, y = 4.dp).width(boxW)) {
                Text(text, style = style, color = color, textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
