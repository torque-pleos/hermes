package com.zigae.hermes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviSpace
import com.zigae.hermes.ui.theme.IviType
import com.zigae.hermes.ui.theme.rainColor

/**
 * 누를 수 있는 면. 누르는 동안 살짝 작아지고(주차 중 화면 전용), 손을 떼면 진동 피드백.
 * 주행 중 화면에서는 쓰지 않는다(애니메이션 금지).
 */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(IviSpace.cardRadius),
    color: Color = Ivi.colors.ivi_surface_card,
    border: Color = Color.Transparent,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = shape,
        color = color,
        border = if (border == Color.Transparent) null else BorderStroke(borderWidth, border),
        content = content,
    )
}

/** 정보 면 (누를 수 없음). 테두리 없이 배경 단차로만 구분한다 */
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
    Text(text, modifier = modifier, style = IviType.label, color = Ivi.colors.ivi_text_secondary, maxLines = 1)
}

/** 작은 배지 (출처·상태) */
@Composable
fun Chip(text: String, color: Color = Ivi.colors.ivi_text_secondary, filled: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(IviSpace.chipRadius))
            .background(color.copy(alpha = if (filled) 0.18f else 0.10f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text, style = IviType.caption, color = color, maxLines = 1)
    }
}

/** 강수 아이콘 + 단계 텍스트 */
@Composable
fun RainBadge(level: RainLevel?, text: String = level?.label ?: "예보 없음", iconSize: Dp = 34.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RainGlyph(level, iconSize)
        Spacer(Modifier.width(IviSpace.s))
        Text(text, style = IviType.bodyStrong, color = Ivi.colors.ivi_text_primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * 강수 아이콘: 구름 + 빗방울 개수(약 1 · 보통 2 · 강 3 · 매우 강 4). 비 없음은 빈 구름 + 체크, 정보 없음은 점선 구름.
 * 색만이 아니라 모양으로도 단계를 구분한다.
 */
@Composable
fun RainGlyph(level: RainLevel?, size: Dp = 40.dp) {
    val color = rainColor(level)
    val ink = if (level == null) Ivi.colors.ivi_text_label else color
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // 구름: 원 세 개 + 바닥 사각
        val cy = h * 0.42f
        val stroke = w * 0.06f
        val cloud = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(Offset(w * 0.10f, cy - h * 0.10f), Size(w * 0.36f, h * 0.30f)))
            addOval(androidx.compose.ui.geometry.Rect(Offset(w * 0.28f, cy - h * 0.28f), Size(w * 0.42f, h * 0.42f)))
            addOval(androidx.compose.ui.geometry.Rect(Offset(w * 0.52f, cy - h * 0.12f), Size(w * 0.36f, h * 0.32f)))
            addRect(androidx.compose.ui.geometry.Rect(Offset(w * 0.24f, cy + h * 0.02f), Size(w * 0.52f, h * 0.18f)))
        }
        drawPath(cloud, ink.copy(alpha = if (level == null) 0.35f else 0.9f), style = Fill)
        val drops = when (level) {
            RainLevel.LIGHT -> 1; RainLevel.MODERATE -> 2; RainLevel.HEAVY -> 3; RainLevel.VERY_HEAVY -> 4; else -> 0
        }
        if (drops > 0) {
            val gap = w * 0.66f / drops
            for (i in 0 until drops) {
                val x = w * 0.22f + gap * (i + 0.5f)
                drawLine(ink, Offset(x, h * 0.70f), Offset(x - w * 0.06f, h * 0.92f), strokeWidth = stroke * 1.4f, cap = StrokeCap.Round)
            }
        }
        if (level == RainLevel.NONE) {
            drawLine(Color.White.copy(alpha = 0.9f), Offset(w * 0.38f, cy + h * 0.02f), Offset(w * 0.47f, cy + h * 0.10f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.9f), Offset(w * 0.47f, cy + h * 0.10f), Offset(w * 0.63f, cy - h * 0.08f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

/** 큰 터치 버튼 (최소 높이 76dp) */
@Composable
fun IviButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = Ivi.colors
    Pressable(
        onClick, modifier.defaultMinSize(minHeight = IviSpace.touchMin),
        shape = RoundedCornerShape(18.dp),
        color = if (primary) c.ivi_accent_primary else c.ivi_surface_card_raised,
        border = if (primary) c.ivi_accent_primary else c.ivi_border_card,
    ) {
        Row(
            Modifier.padding(horizontal = IviSpace.l, vertical = IviSpace.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            val fg = if (primary) c.ivi_accent_on_primary else c.ivi_text_primary
            icon?.let { Icon(it, null, tint = fg, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(IviSpace.s)) }
            Text(text, style = IviType.bodyStrong, color = fg, maxLines = 1)
        }
    }
}

/** 큰 켜기/끄기 스위치 (줄 전체가 터치 대상) */
@Composable
fun BigToggle(title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ivi.colors
    val track by animateColorAsState(if (checked) c.ivi_accent_primary else c.ivi_border_card, label = "track")
    val knobX by animateDpAsState(if (checked) 52.dp else 4.dp, label = "knob")
    Pressable(onToggle, modifier.fillMaxWidth(), color = c.ivi_surface_card_raised) {
        Row(Modifier.padding(IviSpace.l), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = IviType.title, color = c.ivi_text_primary)
                Text(subtitle, style = IviType.caption, color = c.ivi_text_secondary)
            }
            Spacer(Modifier.width(IviSpace.m))
            Box(Modifier.width(104.dp).height(56.dp).clip(RoundedCornerShape(28.dp)).background(track)) {
                Box(
                    Modifier.padding(start = knobX, top = 4.dp).size(48.dp).clip(CircleShape)
                        .background(if (checked) c.ivi_accent_on_primary else c.ivi_surface_card),
                )
            }
        }
    }
}

/**
 * 사실 한 칸: 작은 제목 · 값 · 설명. 누르면 해당 화면으로 가고 오른쪽 꺾쇠로 누를 수 있음을 알린다.
 * 카드 껍데기 없이 한 면 안에 나란히 놓는다(FactRow).
 */
@Composable
fun Fact(
    label: String,
    value: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    valueColor: Color = Ivi.colors.ivi_text_primary,
) {
    val c = Ivi.colors
    Pressable(onClick, modifier.defaultMinSize(minHeight = 132.dp), shape = RoundedCornerShape(IviSpace.cardRadius), color = Color.Transparent) {
        Row(Modifier.padding(horizontal = IviSpace.l, vertical = IviSpace.m), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = IviType.label, color = c.ivi_text_secondary, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    leading?.let { it(); Spacer(Modifier.width(IviSpace.s)) }
                    Text(value, style = IviType.headline, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(detail, style = IviType.caption, color = c.ivi_text_secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Chevron()
        }
    }
}

/** 누를 수 있는 줄의 오른쪽 꺾쇠 */
@Composable
fun Chevron(color: Color = Ivi.colors.ivi_text_label) {
    Canvas(Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        drawLine(color, Offset(w * 0.38f, h * 0.22f), Offset(w * 0.66f, h * 0.5f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.66f, h * 0.5f), Offset(w * 0.38f, h * 0.78f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
    }
}

/** 내비게이션 레일 항목 (아이콘 + 라벨, 최소 높이 112dp) */
@Composable
fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, badge: String? = null) {
    val c = Ivi.colors
    val bg by animateColorAsState(if (selected) c.ivi_accent_primary.copy(alpha = 0.14f) else Color.Transparent, label = "rail")
    Pressable(
        onClick, Modifier.fillMaxWidth().defaultMinSize(minHeight = 112.dp),
        shape = RoundedCornerShape(IviSpace.cardRadius), color = bg,
    ) {
        Column(Modifier.padding(vertical = IviSpace.m), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Icon(icon, label, tint = if (selected) c.ivi_accent_primary else c.ivi_text_secondary, modifier = Modifier.size(40.dp))
                badge?.let {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(start = 28.dp).clip(CircleShape)
                            .background(c.ivi_status_caution).padding(horizontal = 7.dp, vertical = 1.dp),
                    ) { Text(it, style = IviType.label, color = Color.Black) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = IviType.bodyStrong, color = if (selected) c.ivi_text_primary else c.ivi_text_secondary, textAlign = TextAlign.Center)
        }
    }
}

/** 세로 구분선 */
@Composable
fun VDivider() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(Ivi.colors.ivi_border_card))
}

/** 진행 방향 화살표 (북=0°, 시계 방향) */
@Composable
fun HeadingArrow(headingDeg: Double, size: Dp = 40.dp, color: Color = Ivi.colors.ivi_accent_primary) {
    Canvas(Modifier.size(size).rotate(headingDeg.toFloat())) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color.copy(alpha = 0.15f), radius = w / 2, center = Offset(w / 2, h / 2))
        val p = Path().apply {
            moveTo(w / 2, h * 0.08f)
            lineTo(w * 0.82f, h * 0.86f)
            lineTo(w / 2, h * 0.66f)
            lineTo(w * 0.18f, h * 0.86f)
            close()
        }
        drawPath(p, color, style = Fill)
    }
}

/** 가로 구분선 */
@Composable
fun HDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ivi.colors.ivi_border_card))
}
