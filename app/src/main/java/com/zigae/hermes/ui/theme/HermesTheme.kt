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

// 색 방향: 진한 남색 브랜드 + 청록 강조, 강수 단계는 초록→파랑→노랑→주황→빨강 순서
// (사내 HMG 디자인 시스템의 구성 방식을 참고해 Hermes 값으로 새로 정의 — docs/design/README.md)
private val NightColors = IviColors(
    ivi_background_app = Color(0xFF070D1A),
    ivi_surface_card = Color(0xFF0F1A2E),
    ivi_surface_card_raised = Color(0xFF16233C),
    ivi_border_card = Color(0xFF22324F),
    ivi_text_primary = Color(0xFFF4F7FB),
    ivi_text_secondary = Color(0xFFB2BDD0),
    ivi_text_label = Color(0xFF8592AB),
    ivi_accent_primary = Color(0xFF5FD0EC),
    ivi_accent_on_primary = Color(0xFF00222B),
    ivi_status_safe = Color(0xFF5CC77A),
    ivi_status_caution = Color(0xFFFFC266),
    ivi_status_warning = Color(0xFFFF7A70),
    ivi_rain_none = Color(0xFF2F6B45),
    ivi_rain_light = Color(0xFF5B95F8),
    ivi_rain_moderate = Color(0xFFFFC266),
    ivi_rain_heavy = Color(0xFFFF9257),
    ivi_rain_very_heavy = Color(0xFFF0625C),
    ivi_rain_unknown = Color(0xFF3A4762),
)

private val DayColors = IviColors(
    ivi_background_app = Color(0xFFEFF1F4),
    ivi_surface_card = Color(0xFFFFFFFF),
    ivi_surface_card_raised = Color(0xFFF5F6F8),
    ivi_border_card = Color(0xFFD5D9DF),
    ivi_text_primary = Color(0xFF0D1014),
    ivi_text_secondary = Color(0xFF4C525C),
    ivi_text_label = Color(0xFF636A75),
    ivi_accent_primary = Color(0xFF032D60),
    ivi_accent_on_primary = Color(0xFFFFFFFF),
    ivi_status_safe = Color(0xFF1A7F2B),
    ivi_status_caution = Color(0xFF8F5200),
    ivi_status_warning = Color(0xFFB8261F),
    ivi_rain_none = Color(0xFFBFD9C4),
    ivi_rain_light = Color(0xFF2268F0),
    ivi_rain_moderate = Color(0xFFF2A12E),
    ivi_rain_heavy = Color(0xFFD9570F),
    ivi_rain_very_heavy = Color(0xFFC42D2A),
    ivi_rain_unknown = Color(0xFFADB4C0),
)

/** 타이포 토큰. 주행 중 핵심 정보는 큰 크기, 보조 텍스트도 최소 18sp 유지 */
object IviType {
    val hero = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Bold, lineHeight = 60.sp)
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
    /** 터치 대상 최소 높이 (차량용으로 넉넉하게) */
    val touchMin = 76.dp
    val railWidth = 132.dp
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
