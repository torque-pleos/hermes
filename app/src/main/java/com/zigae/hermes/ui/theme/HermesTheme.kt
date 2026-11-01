package com.zigae.hermes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zigae.hermes.radar.RainLevel

/**
 * Hermes 디자인 토큰. Pleos 디자인 토큰 규칙(ivi 접두, 사용 위치가 드러나는 이름)을 따라
 * 각 값의 의미를 이름에 담는다. 주간·야간 두 벌을 두고 시스템 설정을 따른다.
 *
 * 대비: 본문·핵심 텍스트는 배경 대비 4.5:1 이상(Pleos 디자인 원칙).
 */
@Immutable
data class IviColors(
    val ivi_background_app: Color,
    val ivi_surface_card: Color,
    val ivi_surface_card_raised: Color,
    val ivi_border_card: Color,
    val ivi_text_primary: Color,
    val ivi_text_secondary: Color,
    val ivi_text_label: Color,
    val ivi_accent_primary: Color,
    val ivi_accent_on_primary: Color,
    val ivi_status_safe: Color,
    val ivi_status_caution: Color,
    val ivi_status_warning: Color,
    // 강수 단계 (기상청 강도 구분과 같은 순서: 없음 → 매우 강한 비)
    val ivi_rain_none: Color,
    val ivi_rain_light: Color,
    val ivi_rain_moderate: Color,
    val ivi_rain_heavy: Color,
    val ivi_rain_very_heavy: Color,
    /** 예보 없음: 빗금 무늬로 그린다(색만으로 '비 없음'과 구분하지 않음) */
    val ivi_rain_unknown: Color,
)

private val NightColors = IviColors(
    ivi_background_app = Color(0xFF0A1020),
    ivi_surface_card = Color(0xFF111A2E),
    ivi_surface_card_raised = Color(0xFF17223A),
    ivi_border_card = Color(0xFF243150),
    ivi_text_primary = Color(0xFFF2F5FA),
    ivi_text_secondary = Color(0xFFA9B4C8),
    ivi_text_label = Color(0xFF7F8BA3),
    ivi_accent_primary = Color(0xFF4CC3FF),
    ivi_accent_on_primary = Color(0xFF04223A),
    ivi_status_safe = Color(0xFF3DD68C),
    ivi_status_caution = Color(0xFFF5C451),
    ivi_status_warning = Color(0xFFFF6B5E),
    ivi_rain_none = Color(0xFF1F4A44),
    ivi_rain_light = Color(0xFF4FA8F0),
    ivi_rain_moderate = Color(0xFFF5C451),
    ivi_rain_heavy = Color(0xFFFF9B3D),
    ivi_rain_very_heavy = Color(0xFFFF5A5F),
    ivi_rain_unknown = Color(0xFF3A4560),
)

private val DayColors = IviColors(
    ivi_background_app = Color(0xFFF3F5F9),
    ivi_surface_card = Color(0xFFFFFFFF),
    ivi_surface_card_raised = Color(0xFFF7F9FC),
    ivi_border_card = Color(0xFFDDE3EC),
    ivi_text_primary = Color(0xFF0E1726),
    ivi_text_secondary = Color(0xFF475468),
    ivi_text_label = Color(0xFF66738A),
    ivi_accent_primary = Color(0xFF0B6BCB),
    ivi_accent_on_primary = Color(0xFFFFFFFF),
    ivi_status_safe = Color(0xFF13894F),
    ivi_status_caution = Color(0xFF9A6B00),
    ivi_status_warning = Color(0xFFC62B21),
    ivi_rain_none = Color(0xFFBFE3D2),
    ivi_rain_light = Color(0xFF3B8EDB),
    ivi_rain_moderate = Color(0xFFE0A800),
    ivi_rain_heavy = Color(0xFFEA7A12),
    ivi_rain_very_heavy = Color(0xFFD7263D),
    ivi_rain_unknown = Color(0xFFB8C2D1),
)

/** 타이포 토큰. 주행 중 핵심 정보는 큰 크기, 보조 텍스트도 최소 18sp 유지 */
object IviType {
    val display = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp)
    val headline = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
    val title = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)
    val body = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal, lineHeight = 28.sp)
    val bodyStrong = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
    val caption = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp)
    /** 섹션 라벨: 작은 대문자 + 자간 (레퍼런스의 "DRIVER / NAVIGATION" 스타일) */
    val label = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp, fontFamily = FontFamily.Monospace)
    val number = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)
}

object IviSpace {
    val xs = 6.dp
    val s = 10.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val cardRadius = 22.dp
    val chipRadius = 12.dp
    /** 터치 대상 최소 높이 */
    val touchMin = 64.dp
}

val LocalIviColors = staticCompositionLocalOf { NightColors }

object Ivi {
    val colors: IviColors @Composable get() = LocalIviColors.current
}

@Composable
fun HermesTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) NightColors else DayColors
    val material = if (dark) {
        darkColorScheme(primary = c.ivi_accent_primary, onPrimary = c.ivi_accent_on_primary, background = c.ivi_background_app, surface = c.ivi_surface_card)
    } else {
        lightColorScheme(primary = c.ivi_accent_primary, onPrimary = c.ivi_accent_on_primary, background = c.ivi_background_app, surface = c.ivi_surface_card)
    }
    CompositionLocalProvider(LocalIviColors provides c) {
        MaterialTheme(colorScheme = material, content = content)
    }
}

@Composable
fun rainColor(level: RainLevel?): Color {
    val c = Ivi.colors
    return when (level) {
        null -> c.ivi_rain_unknown
        RainLevel.NONE -> c.ivi_rain_none
        RainLevel.LIGHT -> c.ivi_rain_light
        RainLevel.MODERATE -> c.ivi_rain_moderate
        RainLevel.HEAVY -> c.ivi_rain_heavy
        RainLevel.VERY_HEAVY -> c.ivi_rain_very_heavy
    }
}
