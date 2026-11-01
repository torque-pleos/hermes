package com.zigae.hermes.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zigae.hermes.radar.LegRain
import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePointRain

/**
 * 주행 중 화면 (DO). 운전자 주의 분산 가이드: 한눈에 읽히는 2~3줄, 입력·스크롤 없음, 큰 글자.
 * - 다음 정차지와 도착 시각
 * - 다음 구간의 최악 강수(색 블록)와 그 시각
 * - 그다음 구간 요약 한 줄
 */
@Composable
fun DriveScreen(routeRain: List<RoutePointRain>, legs: List<LegRain>, loading: Boolean) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF101418)).padding(48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            Text("강수 예보 불러오는 중", color = Color(0xFF9AA4AE), fontSize = 36.sp)
            return@Column
        }
        val next = legs.firstOrNull()
        if (next == null) {
            val here = routeRain.firstOrNull()?.forecast
            RainRow(level(here), "현재 위치 ${summary(here)}", null)
            Spacer(Modifier.height(24.dp))
            Text("경로 없음 · 목적지를 설정하면 구간 강수를 알려드립니다", color = Color(0xFF9AA4AE), fontSize = 28.sp)
            return@Column
        }
        Text(
            "다음 ${next.to.label} · ${hhmm(next.to.etaEpochSec)} 도착",
            color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(32.dp))
        val worst = next.worst
        RainRow(
            level(worst?.forecast),
            if (worst == null || level(worst.forecast) == RainLevel.NONE) "가는 길 비 예보 없음"
            else "${hhmm(worst.point.etaEpochSec)} 부근 ${level(worst.forecast).label}",
            when {
                (worst?.forecast as? PointForecast.Fleet)?.evidence?.consensus != null ->
                    "주변 차량 ${(worst.forecast as PointForecast.Fleet).evidence.wetVehicles}대가 비 관측"
                worst == null && next.beyondHorizon > 0 -> "예보 범위 밖 구간 있음"
                else -> null
            },
        )
        legs.getOrNull(1)?.let { after ->
            Spacer(Modifier.height(40.dp))
            val w = after.worst
            Text(
                "이후 ${after.to.label} ${hhmm(after.to.etaEpochSec)} · " +
                    (if (w == null) "예보 범위 밖" else if (level(w.forecast) == RainLevel.NONE) "비 예보 없음"
                    else "${hhmm(w.point.etaEpochSec)} 부근 ${level(w.forecast).label}"),
                color = Color(0xFFB8C2CC), fontSize = 28.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RainRow(level: RainLevel, text: String, sub: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(72.dp).background(color(level), RoundedCornerShape(16.dp)))
        Spacer(Modifier.width(28.dp))
        Column {
            Text(text, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            sub?.let { Text(it, color = Color(0xFF9AA4AE), fontSize = 24.sp) }
        }
    }
}

private fun level(f: PointForecast?): RainLevel = f?.rainLevel ?: RainLevel.NONE

private fun summary(f: PointForecast?): String = when (val l = f?.rainLevel) {
    null -> "강수 정보 없음"
    RainLevel.NONE -> "비 없음"
    else -> l.label
}

private fun color(level: RainLevel) = when (level) {
    RainLevel.NONE -> Color(0xFF3A4450)
    RainLevel.LIGHT -> Color(0xFF4FA3E0)
    RainLevel.MODERATE -> Color(0xFFF2C14E)
    RainLevel.HEAVY -> Color(0xFFF28C28)
    RainLevel.VERY_HEAVY -> Color(0xFFE5484D)
}
