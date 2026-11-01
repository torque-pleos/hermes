package com.zigae.hermes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviSpace
import com.zigae.hermes.ui.theme.IviType
import com.zigae.hermes.ui.theme.rainColor

/** 테두리가 있는 카드 (레퍼런스: 남색 바탕 + 얇은 테두리) */
@Composable
fun IviCard(
    modifier: Modifier = Modifier,
    label: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ivi.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(IviSpace.cardRadius),
        color = c.ivi_surface_card,
        border = BorderStroke(1.dp, c.ivi_border_card),
    ) {
        Column(Modifier.padding(IviSpace.l), verticalArrangement = Arrangement.spacedBy(IviSpace.m)) {
            if (label != null || trailing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    label?.let { SectionLabel(it, Modifier.weight(1f)) }
                    trailing?.invoke()
                }
            }
            content()
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Ivi.colors.ivi_accent_primary))
        Text("  $text", style = IviType.label, color = Ivi.colors.ivi_text_label, maxLines = 1)
    }
}

/** 작은 배지 (출처·상태) */
@Composable
fun Chip(text: String, color: Color = Ivi.colors.ivi_text_secondary, filled: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(IviSpace.chipRadius))
            .then(if (filled) Modifier.background(color.copy(alpha = 0.18f)) else Modifier)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(IviSpace.chipRadius))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text, style = IviType.caption, color = color, maxLines = 1)
    }
}

/** 강수 단계 점 + 라벨 */
@Composable
fun RainBadge(level: RainLevel?, text: String = level?.label ?: "예보 없음") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(rainColor(level)))
        Text("  $text", style = IviType.bodyStrong, color = Ivi.colors.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 큰 터치 버튼 (최소 높이 64dp) */
@Composable
fun IviButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true) {
    val c = Ivi.colors
    Box(
        modifier
            .defaultMinSize(minHeight = IviSpace.touchMin)
            .clip(RoundedCornerShape(16.dp))
            .background(if (primary) c.ivi_accent_primary else c.ivi_surface_card_raised)
            .border(1.dp, if (primary) c.ivi_accent_primary else c.ivi_border_card, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = IviSpace.l, vertical = IviSpace.s),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = IviType.bodyStrong, color = if (primary) c.ivi_accent_on_primary else c.ivi_text_primary, maxLines = 1)
    }
}

/** 진행 방향 화살표 (북=0°, 시계 방향) */
@Composable
fun HeadingArrow(headingDeg: Double, size: Dp = 40.dp, color: Color = Ivi.colors.ivi_accent_primary) {
    Canvas(Modifier.size(size).rotate(headingDeg.toFloat())) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w / 2, 0f)
            lineTo(w * 0.9f, h * 0.9f)
            lineTo(w / 2, h * 0.68f)
            lineTo(w * 0.1f, h * 0.9f)
            close()
        }
        drawPath(p, color, style = Fill)
        drawCircle(color.copy(alpha = 0.15f), radius = w / 2, center = Offset(w / 2, h / 2))
    }
}
