package com.zigae.hermes.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.constant.control.WiperSwitch
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
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

data class VehicleSnapshot(
    val wiperState: WiperState? = null,
    val wiperSwitch: WiperSwitch? = null,
    val wiperStateEvents: Int = 0,
    val gear: VehicleGear? = null,
    /** 도어 영역 이름 → 잠김 여부 */
    val doorLocks: Map<String, Boolean?> = emptyMap(),
    val lastError: String? = null,
    val updatedAt: Long = 0L,
)

/**
 * Vehicle State SDK(조회 전용) 래퍼.
 * Vehicle SDK는 연결 완료 콜백이 없어서 주기 조회 + 와이퍼 상태 리스너를 함께 쓴다.
 */
class VehicleMonitor(context: Context, private val scope: CoroutineScope) {

    private val vehicle = Vehicle(context.applicationContext)
    private val frontWindshield = WindowArea(WindowValue.FRONT_WINDSHIELD)
    private var doorAreas: List<DoorArea> = emptyList()
    private var pollJob: Job? = null
    private var listenerRegistered = false

    private val _state = MutableStateFlow(VehicleSnapshot())
    val state: StateFlow<VehicleSnapshot> = _state.asStateFlow()

    private val wiperListener = object : WiperStateListener {
        override fun onWiperStateUpdated(state: WiperState?) {
            Log.i(TAG, "wiper state event: $state")
            _state.update {
                it.copy(
                    wiperState = state,
                    wiperStateEvents = it.wiperStateEvents + 1,
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }

        override fun onFailed(e: Exception) = reportError("wiper listener", e)
    }

    fun start() {
        vehicle.initialize()
        pollJob = scope.launch {
            while (isActive) {
                poll()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        if (listenerRegistered) {
            runCatching { vehicle.getWiper().unregisterWiperState(frontWindshield, wiperListener) }
        }
        vehicle.release()
    }

    private fun poll() {
        val wiper = vehicle.getWiper()
        wiper.getCurrentWiperSwitch(
            frontWindshield,
            { value -> _state.update { it.copy(wiperSwitch = value, updatedAt = now()) } },
            { e -> reportError("wiper switch", e) },
        )
        wiper.getCurrentWiperState(
            frontWindshield,
            { value -> _state.update { it.copy(wiperState = value, updatedAt = now()) } },
            { e -> reportError("wiper state", e) },
        )
        @Suppress("DEPRECATION")
        vehicle.getGear().getCurrentGearSelectionState(
            { value -> _state.update { it.copy(gear = value, updatedAt = now()) } },
            { e -> reportError("gear", e) },
        )

        if (!listenerRegistered) {
            runCatching { wiper.registerWiperState(frontWindshield, wiperListener) }
                .onSuccess { listenerRegistered = true }
                .onFailure { reportError("wiper register", it) }
        }

        if (doorAreas.isEmpty()) {
            vehicle.getDoor().checkDoorLockCapabilities(
                { _, areas -> doorAreas = areas },
                { e -> reportError("door capabilities", e) },
            )
        }
        doorAreas.forEach { area ->
            val name = area.values.joinToString("+") { it.name }
            vehicle.getDoor().getDoorLockState(
                area,
                { locked -> _state.update { it.copy(doorLocks = it.doorLocks + (name to locked), updatedAt = now()) } },
                { e -> reportError("door $name", e) },
            )
        }
    }

    private fun reportError(what: String, e: Throwable) {
        // VehicleException은 배포 AAR에서 난독화되어 있어 errorCode를 쓸 수 없다 → 메시지만 사용.
        val msg = "$what: ${e.message ?: e.javaClass.simpleName}"
        Log.w(TAG, msg)
        _state.update { it.copy(lastError = msg) }
    }

    private fun now() = System.currentTimeMillis()

    private companion object {
        const val TAG = "Hermes/Vehicle"
        const val POLL_INTERVAL_MS = 2_000L
    }
}
