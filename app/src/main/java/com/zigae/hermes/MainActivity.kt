package com.zigae.hermes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.zigae.hermes.navi.NaviMonitor
import com.zigae.hermes.ui.DiagnosticsScreen
import com.zigae.hermes.vehicle.VehicleMonitor
import com.zigae.hermes.voice.Speaker

class MainActivity : ComponentActivity() {

    private lateinit var vehicleMonitor: VehicleMonitor
    private lateinit var naviMonitor: NaviMonitor
    private lateinit var speaker: Speaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vehicleMonitor = VehicleMonitor(this, lifecycleScope).also { it.start() }
        naviMonitor = NaviMonitor(this, lifecycleScope).also { it.start() }
        speaker = Speaker(this)

        setContent {
            val vehicle by vehicleMonitor.state.collectAsStateWithLifecycle()
            val navi by naviMonitor.state.collectAsStateWithLifecycle()
            val tts by speaker.status.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface {
                    DiagnosticsScreen(
                        vehicle = vehicle,
                        navi = navi,
                        ttsStatus = tts,
                        onRequestTestRoute = naviMonitor::requestTestRoute,
                        onSpeak = { speaker.speak("전방 30분 뒤 강한 비가 예상됩니다. 상차 일정을 확인하세요.") },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        speaker.shutdown()
        naviMonitor.stop()
        vehicleMonitor.stop()
        super.onDestroy()
    }
}
