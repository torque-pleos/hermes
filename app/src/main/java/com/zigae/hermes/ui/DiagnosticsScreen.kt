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
import com.zigae.hermes.fleet.FleetState
import com.zigae.hermes.navi.NaviSnapshot
import com.zigae.hermes.radar.LegRain
import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.RouteRain
import com.zigae.hermes.radar.RadarState
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePointRain
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.zigae.hermes.vehicle.VehicleSnapshot
import com.zigae.hermes.vehicle.toRain
import com.zigae.hermes.weather.KmaState

/** 개발 단계 진단 화면: Hermes가 쓰는 SDK 값이 실제로 들어오는지 확인한다. */
@Composable
fun DiagnosticsScreen(
    vehicle: VehicleSnapshot,
    navi: NaviSnapshot,
    radar: RadarState,
    kma: KmaState,
    routeRain: List<RoutePointRain>,
    legs: List<LegRain>,
    fleet: FleetState,
    onRequestTestRoute: () -> Unit,
    onToggleConsent: () -> Unit,
    onSeedDemo: () -> Unit,
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
                Spacer(Modifier.height(16.dp))
                FleetSection(fleet, onToggleConsent, onSeedDemo)
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
            Panel("경로 강수 (레이더 + 기상청)") {
                RadarSection(radar, kma, routeRain, legs, vehicle)
            }
        }
    }
}

@Composable
private fun ColumnScope.FleetSection(fleet: FleetState, onToggleConsent: () -> Unit, onSeedDemo: () -> Unit) {
    Text("차량 간 와이퍼 공유", fontWeight = FontWeight.Bold, fontSize = 22.sp)
    if (!fleet.configured) {
        Line("상태", "서버 설정 없음 (FLEET_URL/KEY)")
        return
    }
    val now = System.currentTimeMillis() / 1000
    Line("공유 동의", if (fleet.consent) "켜짐 (위치 약 500m 단위 · 와이퍼 단계)" else "꺼짐")
    val demo = fleet.reports.count { it.demo }
    val vehicles = fleet.reports.map { it.vehicleId }.distinct().size
    Line("주변 관측", "최근 20분 ${vehicles}대 (가상 ${fleet.reports.filter { it.demo }.map { it.vehicleId }.distinct().size}대, 관측 ${fleet.reports.size}건 중 $demo)")
    if (fleet.lastUploadEpochSec > 0) Line("내 관측 업로드", "${(now - fleet.lastUploadEpochSec) / 60}분 전")
    fleet.error?.let { Line("오류", it) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onToggleConsent) { Text(if (fleet.consent) "공유 끄기" else "공유 동의", fontSize = 18.sp) }
        Button(onClick = onSeedDemo) { Text("시연: 경로 앞 가상 차량 비 관측", fontSize = 18.sp) }
    }
}

// NaviHelper 단위: remainDistance = m, remainTime = 초 (에뮬레이터 확인)
private fun km(meters: Int) = "%.1fkm".format(meters / 1000.0)
private fun min(seconds: Int) = if (seconds >= 3600) "${seconds / 3600}시간 ${seconds % 3600 / 60}분" else "${seconds / 60}분"

private val clock = SimpleDateFormat("HH:mm", Locale.KOREA)
internal fun hhmm(epochSec: Long): String = clock.format(Date(epochSec * 1000))

private val compass = listOf("북", "북동", "동", "남동", "남", "남서", "서", "북서")
private fun compass(deg: Double) = compass[((deg + 22.5) / 45).toInt() % 8]

@Composable
private fun ColumnScope.RadarSection(
    radar: RadarState,
    kma: KmaState,
    routeRain: List<RoutePointRain>,
    legs: List<LegRain>,
    vehicle: VehicleSnapshot,
) {
    val now = System.currentTimeMillis() / 1000
    Line(
        "기상청 예보",
        when {
            !kma.enabled -> "키 없음 → 레이더만 사용"
            kma.updatedAtEpochSec == 0L -> "불러오는 중"
            else -> "격자 ${kma.cells}개 · ${hhmm(kma.updatedAtEpochSec)} 확인" + (kma.error?.let { " · $it" } ?: "")
        },
    )
    val cast = radar.nowcaster
    if (cast == null) {
        Line("레이더", if (radar.loading) "불러오는 중" else (radar.error ?: "위치 대기 중"))
    } else {
        val frame = cast.latest
        Line("레이더 관측", "${hhmm(frame.timeEpochSec)} (${(now - frame.timeEpochSec) / 60}분 전)")
        Line("레이더 영역", "경로 타일 ${radar.tileCount}개 · 프레임 ${radar.framesUsed}개")
        val m = cast.motion
        val lat = routeRain.firstOrNull()?.point?.lat ?: 37.0
        Line(
            "비구름 이동",
            if (m.confidence <= 0.0) "추정 불가 (에코 부족)"
            else "${compass(m.headingDeg())}쪽 %.0fkm/h · 신뢰도 %.0f%%".format(m.speedKmh(lat, frame.zoom), m.confidence * 100) +
                if (cast.field != null) " · 지역별 보정" else "",
        )
        radar.error?.let { Line("레이더 오류", it) }
    }
    Spacer(Modifier.height(8.dp))
    if (cast == null && kma.updatedAtEpochSec == 0L) {
        Line("예보", "불러오는 중")
        return
    }
    routeRain.forEach { (p, f) -> Line("${p.label} ${hhmm(p.etaEpochSec)}", describe(f)) }

    // 구간(직선 근사) 최악 강수
    if (legs.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("구간 (직선 근사, ${(RouteRain.SAMPLE_SPACING_M / 1000).toInt()}km 간격)", fontWeight = FontWeight.Bold)
    }
    legs.forEach { leg ->
        val worst = leg.worst
        val head = "${leg.from.label.substringBefore(' ')}→${leg.to.label.substringBefore(' ')} " +
            "${hhmm(leg.from.etaEpochSec)}~${hhmm(leg.to.etaEpochSec)}"
        val body = buildString {
            if (worst == null) append("예측 가능 구간 없음")
            else append("최대 ${describe(worst.forecast)} · ${hhmm(worst.point.etaEpochSec)} 부근")
            if (leg.beyondHorizon > 0) append(" · 예측 범위 밖 ${leg.beyondHorizon}/${leg.samples.size}")
        }
        Line(head, body)
    }

    // F4 미리보기: 현재 위치 레이더 vs 와이퍼
    // 차량 보정 전의 레이더 판단과 내 와이퍼를 비교한다
    val here = routeRain.firstOrNull()?.forecast?.let { (it as? PointForecast.Fleet)?.base ?: it } as? PointForecast.Rain
    Spacer(Modifier.height(8.dp))
    Line("교차 검증", "레이더 ${here?.level?.label ?: "-"} / 와이퍼 ${vehicle.wiperSwitch.toRain().label}")
}

private fun describe(f: PointForecast): String = when (f) {
    is PointForecast.Rain ->
        if (f.level == RainLevel.NONE) "비 없음" else "${f.level.label} %.1fmm/h (%.0fdBZ)".format(f.rateMmPerHour, f.dbz)
    is PointForecast.Kma ->
        "기상청 " + (if (f.level == RainLevel.NONE) "비 없음" else "${f.level.label}${if (f.snow) "(눈)" else ""} %.1fmm/h".format(f.rateMmPerHour)) +
            (f.pop?.let { " · 확률 $it%" } ?: "")
    is PointForecast.Fleet -> {
        val e = f.evidence
        val changed = f.level != f.base.rainLevel
        (if (changed) "차량 보정 ${f.level?.label ?: "-"} ← " else "") + describe(f.base) +
            " · 주변 차량 ${e.vehicles}대(비 ${e.wetVehicles})"
    }
    is PointForecast.BeyondHorizon ->
        "예측 범위 밖 (+${f.leadMinutes}분) · 지금 그곳: ${f.observedNow?.label ?: "레이더 영역 밖"}"
    PointForecast.OutOfCoverage -> "레이더 영역 밖"
}

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
