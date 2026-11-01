package com.zigae.hermes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.zigae.hermes.fleet.FleetForecaster
import com.zigae.hermes.fleet.FleetFusion
import com.zigae.hermes.fleet.FleetReport
import com.zigae.hermes.fleet.FleetRepository
import com.zigae.hermes.fleet.Wind
import com.zigae.hermes.navi.NaviMonitor
import com.zigae.hermes.navi.NaviSnapshot
import com.zigae.hermes.radar.Nowcaster
import com.zigae.hermes.radar.RadarRepository
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePoint
import com.zigae.hermes.radar.RouteRain
import com.zigae.hermes.ui.DiagnosticsScreen
import com.zigae.hermes.ui.DriveScreen
import com.zigae.hermes.ui.AppShell
import com.zigae.hermes.ui.BriefingScreen
import com.zigae.hermes.ui.FleetScreen
import com.zigae.hermes.ui.RouteScreen
import com.zigae.hermes.ui.Tab
import com.zigae.hermes.ui.theme.HermesTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.zigae.hermes.vehicle.DrivingState
import com.zigae.hermes.vehicle.VehicleMonitor
import com.zigae.hermes.vehicle.VehicleSnapshot
import com.zigae.hermes.vehicle.toRain
import com.zigae.hermes.vehicle.toRainLevel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.zigae.hermes.weather.BlendedForecaster
import com.zigae.hermes.weather.KmaRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private companion object {
        /** 경로 주변 관측 조회 여유(약 50km) */
        const val FETCH_MARGIN_DEG = 0.5
        const val DEMO_VEHICLES = 5
    }

    private lateinit var vehicleMonitor: VehicleMonitor
    private lateinit var naviMonitor: NaviMonitor
    private lateinit var radar: RadarRepository
    private val kma = KmaRepository(BuildConfig.KMA_SERVICE_KEY)
    private lateinit var drivingState: DrivingState
    private lateinit var fleet: FleetRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vehicleMonitor = VehicleMonitor(this, lifecycleScope).also { it.start() }
        drivingState = DrivingState(this)
        naviMonitor = NaviMonitor(this, lifecycleScope).also { it.start() }
        radar = RadarRepository(lifecycleScope, pointsProvider = {
            RouteRain.allSamples(routePoints(naviMonitor.state.value))
        }).also { it.start() }
        fleet = FleetRepository(this, BuildConfig.FLEET_URL, BuildConfig.FLEET_KEY)
        // 차량 간 와이퍼 관측: 1분마다 내 관측 올리기 + 경로 주변 관측 받기
        lifecycleScope.launch {
            while (true) {
                val samples = RouteRain.allSamples(routePoints(naviMonitor.state.value))
                if (samples.isNotEmpty()) {
                    uploadOwn()
                    fleet.fetch(
                        samples.minOf { it.lat } - FETCH_MARGIN_DEG, samples.maxOf { it.lat } + FETCH_MARGIN_DEG,
                        samples.minOf { it.lon } - FETCH_MARGIN_DEG, samples.maxOf { it.lon } + FETCH_MARGIN_DEG,
                    )
                }
                delay(if (samples.isEmpty()) 2_000 else 60_000)
            }
        }
        // 와이퍼 단계가 바뀌면 바로 올린다
        lifecycleScope.launch {
            vehicleMonitor.state.map { it.wiperSwitch }.distinctUntilChanged().collect { uploadOwn() }
        }
        // 기상청 예보: 경로 표본 격자를 1분마다 확인 (발표 시각이 바뀐 격자만 실제 호출)
        lifecycleScope.launch {
            while (true) {
                val samples = RouteRain.allSamples(routePoints(naviMonitor.state.value))
                if (samples.isNotEmpty()) kma.refresh(samples.map { it.lat to it.lon })
                // 새 격자·발표 시각이 바뀐 격자만 실제 호출하므로 자주 확인해도 비용이 거의 없다
                delay(if (samples.isEmpty()) 2_000 else 10_000)
            }
        }

        setContent {
            val vehicle by vehicleMonitor.state.collectAsStateWithLifecycle()
            val navi by naviMonitor.state.collectAsStateWithLifecycle()
            val radarState by radar.state.collectAsStateWithLifecycle()
            val kmaState by kma.state.collectAsStateWithLifecycle()
            val driving by drivingState.requiresDistractionOptimization.collectAsStateWithLifecycle()
            val fleetState by fleet.state.collectAsStateWithLifecycle()
            // 남은 시간 기반 ETA와 "n분 전" 표시를 갱신하기 위한 틱
            var tick by remember { mutableLongStateOf(0L) }
            LaunchedEffect(Unit) { while (true) { delay(30_000); tick++ } }

            val points = tick.let { routePoints(navi) }
            val now = System.currentTimeMillis() / 1000
            val nowcaster = radarState.nowcaster
            val forecaster = FleetForecaster(
                BlendedForecaster(nowcaster, kma::hourly, now),
                fleetState.reports + listOfNotNull(ownReport(vehicle, navi, now)),
                now,
                nowcaster?.let { cast -> Wind { lat, lon -> cast.velocityAt(lat, lon) } },
            )
            val routeRain = RouteRain.forecast(forecaster, points)
            val legs = RouteRain.legs(forecaster, points)

            var tab by remember { mutableStateOf(Tab.BRIEFING) }
            var selectedStop by remember { mutableStateOf(1) }
            // 개발·발표용: 주차 중에도 주행 화면을 확인 (실제 주행 중에는 규제 상태로 자동 전환)
            var previewDrive by remember { mutableStateOf(false) }
            val fleetWet = fleetState.reports.groupBy { it.vehicleId }.values
                .count { rs -> rs.maxBy { it.observedAtEpochSec }.level > RainLevel.NONE }
            HermesTheme {
                Surface {
                    if (driving || previewDrive) {
                        Box(Modifier.fillMaxSize().clickable(enabled = previewDrive && !driving) { previewDrive = false }) {
                            DriveScreen(routeRain, legs, radarState.nowcaster == null && kmaState.updatedAtEpochSec == 0L)
                        }
                    } else {
                        AppShell(tab, { tab = it }, fleetWet) {
                            when (tab) {
                                Tab.BRIEFING -> BriefingScreen(
                                    routeRain = routeRain, legs = legs, radar = radarState, kma = kmaState, fleet = fleetState,
                                    onOpenStop = { selectedStop = it; tab = Tab.ROUTE },
                                    onOpenFleet = { tab = Tab.FLEET },
                                    onRequestTestRoute = naviMonitor::requestTestRoute,
                                )
                                Tab.ROUTE -> RouteScreen(
                                    routeRain = routeRain, legs = legs, radar = radarState,
                                    selected = selectedStop, onSelect = { selectedStop = it },
                                    onRequestTestRoute = naviMonitor::requestTestRoute,
                                )
                                Tab.FLEET -> FleetScreen(
                                    fleet = fleetState, vehicle = vehicle, routeRain = routeRain,
                                    onToggleConsent = { fleet.setConsent(!fleetState.consent) },
                                    onSeedDemo = { lifecycleScope.launch { seedDemo(points, nowcaster) } },
                                )
                                Tab.DIAGNOSTICS -> DiagnosticsScreen(
                                    vehicle = vehicle,
                                    navi = navi,
                                    radar = radarState,
                                    kma = kmaState,
                                    routeRain = routeRain,
                                    legs = legs,
                                    fleet = fleetState,
                                    onRequestTestRoute = naviMonitor::requestTestRoute,
                                    onToggleConsent = { fleet.setConsent(!fleetState.consent) },
                                    onSeedDemo = { lifecycleScope.launch { seedDemo(points, nowcaster) } },
                                    onClose = { tab = Tab.BRIEFING },
                                    onPreviewDrive = { previewDrive = true },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        drivingState.release()
        radar.stop()
        naviMonitor.stop()
        vehicleMonitor.stop()
        super.onDestroy()
    }

    /** 내 차 와이퍼 관측 (서버를 거치지 않고 판단에 바로 쓴다). 강도를 모르면 null */
    private fun ownReport(vehicle: VehicleSnapshot, navi: NaviSnapshot, now: Long): FleetReport? {
        val loc = navi.location ?: return null
        val level = vehicle.wiperSwitch.toRain().toRainLevel() ?: return null
        return FleetReport(fleet.vehicleId, loc.latitude, loc.longitude, now, level)
    }

    private suspend fun uploadOwn() {
        val loc = naviMonitor.state.value.location ?: return
        val v = vehicleMonitor.state.value
        fleet.upload(loc.latitude, loc.longitude, v.wiperSwitch.toRain().toRainLevel(), v.wiperSwitch?.name)
    }

    /**
     * 시연: 경로상 20~40분 뒤 지날 구간에 비가 오도록, 비구름이 오는 쪽(상류)에 비를 보는 가상 차량 관측을 만든다.
     */
    private suspend fun seedDemo(points: List<RoutePoint>, nowcaster: Nowcaster?) {
        val now = System.currentTimeMillis() / 1000
        val window = RouteRain.allSamples(points).filter { (it.etaEpochSec - now) / 60 in 20..40 }
        val target = window.ifEmpty { RouteRain.allSamples(points).take(3) }
        if (target.isEmpty()) return
        fun upwind(p: RoutePoint): Pair<Double, Double> {
            val v = nowcaster?.velocityAt(p.lat, p.lon)
            val at = FleetFusion.advect(p.lat, p.lon, -(p.etaEpochSec - now).toDouble(), v)
            return at.lat to at.lon
        }
        val (lat1, lon1) = upwind(target.first())
        val (lat2, lon2) = upwind(target.last())
        fleet.seedDemo(lat1, lon1, lat2, lon2, RainLevel.HEAVY, DEMO_VEHICLES)
        val s = RouteRain.allSamples(points)
        fleet.fetch(s.minOf { it.lat } - FETCH_MARGIN_DEG, s.maxOf { it.lat } + FETCH_MARGIN_DEG,
            s.minOf { it.lon } - FETCH_MARGIN_DEG, s.maxOf { it.lon } + FETCH_MARGIN_DEG)
    }

    /** 현재 위치(지금) → 경유지들 → 목적지 순서의 예측 대상. */
    private fun routePoints(navi: NaviSnapshot): List<RoutePoint> {
        val now = System.currentTimeMillis() / 1000
        val here = navi.location ?: return emptyList()
        val points = mutableListOf(RoutePoint("현재 위치", here.latitude, here.longitude, now))
        navi.waypoints.forEachIndexed { i, w ->
            points += RoutePoint("경유 ${i + 1} ${w.name}", w.latitude, w.longitude, now + w.remainTime)
        }
        navi.destination?.takeIf { it.name.isNotBlank() }?.let { d ->
            points += RoutePoint("도착 ${d.name}", d.latitude, d.longitude, now + d.remainTime)
        }
        // 좌표를 모르는 지점(0,0)은 예측할 수 없으므로 제외
        return points.filterNot { it.lat == 0.0 && it.lon == 0.0 }
    }
}
