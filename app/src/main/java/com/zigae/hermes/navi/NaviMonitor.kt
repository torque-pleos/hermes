package com.zigae.hermes.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.RouteWithWaypointsRequest
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import com.zigae.hermes.geo.LatLon
import com.zigae.hermes.geo.WaypointLocator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NaviSnapshot(
    val connected: Boolean = false,
    val location: CurrentLocationInfo? = null,
    val routeState: RouteStateInfo? = null,
    val destination: DestinationInfo? = null,
    val waypoints: List<WaypointInfo> = emptyList(),
    val lastEvent: String? = null,
    val lastError: String? = null,
)

/** NaviHelper 래퍼: 현재 위치, 경로 상태, 목적지/경유지 남은 시간을 주기 조회한다. */
class NaviMonitor(context: Context, private val scope: CoroutineScope) {

    private val navi = NaviHelper(context.applicationContext)

    /**
     * 에뮬레이터 NaviHelper는 WaypointInfo 좌표를 0.0으로 돌려준다(이름·주소·남은 거리·시간은 정상).
     * 요청 좌표 → 저장된 결과 → 주소 지오코딩 순으로 보완한다.
     * (requestTrip의 TripInfo.routePoints는 경유지를 빼고 목적지까지 새로 계산한 경로라 쓸 수 없음 — 에뮬레이터 확인)
     */
    private val locator = WaypointLocator(context)
    private val geocoding = mutableSetOf<String>()

    private fun withKnownCoords(w: WaypointInfo): WaypointInfo {
        if (w.latitude != 0.0 || w.longitude != 0.0) return w
        val at = locator.cached(w.name, w.address) ?: run {
            resolveLater(w)
            return w
        }
        return w.copy(latitude = at.lat, longitude = at.lon)
    }

    private fun resolveLater(w: WaypointInfo) {
        val key = "${w.name}|${w.address}"
        if (!geocoding.add(key)) return
        scope.launch {
            val here = _state.value.location?.let { LatLon(it.latitude, it.longitude) }
            val at = locator.locate(w.name, w.address, here, w.remainDistance)
            geocoding.remove(key)
            if (at != null) _state.update { s -> s.copy(waypoints = s.waypoints.map(::withKnownCoords)) }
        }
    }
    private var pollJob: Job? = null

    private val _state = MutableStateFlow(NaviSnapshot())
    val state: StateFlow<NaviSnapshot> = _state.asStateFlow()

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            _state.update { it.copy(location = currentLocationInfo) }
        }

        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            _state.update { it.copy(routeState = routeStateInfo) }
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            if (destinationInfo != _state.value.destination) Log.i(TAG, "destination: $destinationInfo")
            _state.update { it.copy(destination = destinationInfo) }
        }

        override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
            if (waypointInfoList.map(::withKnownCoords) != _state.value.waypoints) Log.i(TAG, "waypoints: $waypointInfoList")
            _state.update { it.copy(waypoints = waypointInfoList.map(::withKnownCoords)) }
        }

        override fun onRoutingCompleted() = event("routing completed")
        override fun onRouteCancelled() = event("route cancelled")
        override fun onRouteEnded() = event("route ended")

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "navi error: $errorCode")
            _state.update { it.copy(lastError = errorCode.name) }
        }
    }

    fun start() {
        navi.addListener(listener)
        navi.initialize(
            onConnected = {
                Log.i(TAG, "connected")
                _state.update { it.copy(connected = true, lastError = null) }
                startPolling()
            },
            onFailed = { t ->
                Log.w(TAG, "connect failed", t)
                _state.update { it.copy(connected = false, lastError = "connect: ${t.message}") }
            },
        )
    }

    fun stop() {
        pollJob?.cancel()
        navi.removeListener(listener)
        navi.release()
    }

    /** 경유지 ETA 확인용 테스트 경로: 현재 위치 → 대전(경유) → 부산 신항. */
    fun requestTestRoute() {
        val destination = RouteInfo(
            latitude = 35.0765, longitude = 128.8203,
            poiName = "부산신항", poiId = "", address = "부산 강서구 성북동", poiSubId = "",
        )
        val waypoint = RequestWaypointInfo(
            latitude = 36.3504, longitude = 127.3845,
            poiName = "대전 물류센터", waypointIndex = WaypointIndex.FIRST,
            poiId = "", address = "대전 서구", poiSubId = "",
        )
        // 내비가 돌려주는 주소는 요청 주소와 달라서 이름으로만 기억한다
        locator.remember(waypoint.poiName.orEmpty(), null, LatLon(waypoint.latitude, waypoint.longitude))
        runCatching { navi.requestRouteWithWaypoints(RouteWithWaypointsRequest(destination, listOf(waypoint))) }
            .onSuccess { event("test route requested") }
            .onFailure { t -> _state.update { it.copy(lastError = "route: ${t.message}") } }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                runCatching {
                    navi.getCurrentLocationInfo()
                    navi.getRouteStateInfo()
                    navi.getDestinationInfo()
                    navi.getWaypointInfo()
                }.onFailure { t -> _state.update { it.copy(lastError = "poll: ${t.message}") } }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun event(name: String) {
        Log.i(TAG, name)
        _state.update { it.copy(lastEvent = name) }
    }

    private companion object {
        const val TAG = "Hermes/Navi"
        const val POLL_INTERVAL_MS = 3_000L
    }
}
