package com.zigae.hermes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.zigae.hermes.navi.NaviMonitor
import com.zigae.hermes.navi.NaviSnapshot
import com.zigae.hermes.radar.RadarRepository
import com.zigae.hermes.radar.RoutePoint
import com.zigae.hermes.radar.RouteRain
import com.zigae.hermes.ui.DiagnosticsScreen
import com.zigae.hermes.vehicle.VehicleMonitor
import com.zigae.hermes.weather.BlendedForecaster
import com.zigae.hermes.weather.KmaRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var vehicleMonitor: VehicleMonitor
    private lateinit var naviMonitor: NaviMonitor
    private lateinit var radar: RadarRepository
    private val kma = KmaRepository(BuildConfig.KMA_SERVICE_KEY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vehicleMonitor = VehicleMonitor(this, lifecycleScope).also { it.start() }
        naviMonitor = NaviMonitor(this, lifecycleScope).also { it.start() }
        radar = RadarRepository(lifecycleScope, pointsProvider = {
            RouteRain.allSamples(routePoints(naviMonitor.state.value))
        }).also { it.start() }
        // 기상청 예보: 경로 표본 격자를 1분마다 확인 (발표 시각이 바뀐 격자만 실제 호출)
        lifecycleScope.launch {
            while (true) {
                val samples = RouteRain.allSamples(routePoints(naviMonitor.state.value))
                if (samples.isNotEmpty()) kma.refresh(samples.map { it.lat to it.lon })
                delay(if (samples.isEmpty()) 2_000 else 60_000)
            }
        }

        setContent {
            val vehicle by vehicleMonitor.state.collectAsStateWithLifecycle()
            val navi by naviMonitor.state.collectAsStateWithLifecycle()
            val radarState by radar.state.collectAsStateWithLifecycle()
            val kmaState by kma.state.collectAsStateWithLifecycle()
            // 남은 시간 기반 ETA와 "n분 전" 표시를 갱신하기 위한 틱
            var tick by remember { mutableLongStateOf(0L) }
            LaunchedEffect(Unit) { while (true) { delay(30_000); tick++ } }

            val points = tick.let { routePoints(navi) }
            val forecaster = BlendedForecaster(
                radarState.nowcaster, kma::hourly, System.currentTimeMillis() / 1000,
            )
            val routeRain = RouteRain.forecast(forecaster, points)
            val legs = RouteRain.legs(forecaster, points)

            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface {
                    DiagnosticsScreen(
                        vehicle = vehicle,
                        navi = navi,
                        radar = radarState,
                        kma = kmaState,
                        routeRain = routeRain,
                        legs = legs,
                        onRequestTestRoute = naviMonitor::requestTestRoute,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        radar.stop()
        naviMonitor.stop()
        vehicleMonitor.stop()
        super.onDestroy()
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
