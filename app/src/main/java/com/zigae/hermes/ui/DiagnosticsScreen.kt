package com.zigae.hermes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zigae.hermes.navi.NaviSnapshot
import com.zigae.hermes.vehicle.VehicleSnapshot
import com.zigae.hermes.vehicle.toRain

/** 개발 단계 진단 화면: Hermes가 쓰는 SDK 값이 실제로 들어오는지 확인한다. */
@Composable
fun DiagnosticsScreen(
    vehicle: VehicleSnapshot,
    navi: NaviSnapshot,
    ttsStatus: String,
    onRequestTestRoute: () -> Unit,
    onSpeak: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Hermes · SDK 진단", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Panel("차량 (Vehicle State)") {
                Line("와이퍼 스위치", vehicle.wiperSwitch?.name ?: "-")
                Line("→ 추정 강수", vehicle.wiperSwitch.toRain().label)
                Line("와이퍼 상태", vehicle.wiperState?.name ?: "-")
                Line("상태 이벤트 수", vehicle.wiperStateEvents.toString())
                Line("기어", vehicle.gear?.name ?: "-")
                if (vehicle.doorLocks.isEmpty()) Line("도어 잠금", "-")
                vehicle.doorLocks.forEach { (area, locked) ->
                    Line("도어 $area", when (locked) { true -> "잠김"; false -> "열림"; null -> "-" })
                }
                Line("오류", vehicle.lastError ?: "없음")
            }
            Panel("내비 (NaviHelper)") {
                Line("연결", if (navi.connected) "됨" else "안 됨")
                val loc = navi.location
                Line("현재 위치", loc?.let { "%.5f, %.5f".format(it.latitude, it.longitude) } ?: "-")
                Line("주소", loc?.address?.ifBlank { null } ?: "-")
                Line("경로 안내 중", navi.routeState?.isRouting?.toString() ?: "-")
                navi.destination?.let {
                    Line("목적지", "${it.name} · ${km(it.remainDistance)} · ${min(it.remainTime)}")
                } ?: Line("목적지", "-")
                if (navi.waypoints.isEmpty()) Line("경유지", "-")
                navi.waypoints.forEachIndexed { i, w ->
                    Line("경유지 ${i + 1}", "${w.name} · ${km(w.remainDistance)} · ${min(w.remainTime)}")
                }
                Line("이벤트", navi.lastEvent ?: "-")
                Line("오류", navi.lastError ?: "없음")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRequestTestRoute) { Text("테스트 경로 설정 (대전 경유 → 부산신항)", fontSize = 20.sp) }
            }
            Panel("음성 (Android TTS)") {
                Line("상태", ttsStatus)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSpeak) { Text("안내 음성 테스트", fontSize = 20.sp) }
            }
        }
    }
}

// 단위 미확인: 실제 값을 보고 m/초 여부를 확정한다.
private fun km(meters: Int) = "${meters}m"
private fun min(seconds: Int) = "${seconds}s"

@Composable
private fun androidx.compose.foundation.layout.RowScope.Panel(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(Modifier.weight(1f).fillMaxHeight()) {
        Column(
            Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row {
        Text(label, fontSize = 20.sp, modifier = Modifier.weight(0.4f))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f))
    }
}
