package com.zigae.hermes.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AAOS 주행 규제 상태. Pleos 가이드대로 CarUxRestrictions의 DO 필요 여부로 주행 중 UI를 전환한다.
 */
class DrivingState(context: Context) {

    private val _requiresDo = MutableStateFlow(false)
    /** true면 주행 중 → 단순화된 운행 화면만 보여야 한다 */
    val requiresDistractionOptimization: StateFlow<Boolean> = _requiresDo.asStateFlow()

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    private val listener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { apply(it) }

    init {
        runCatching {
            car = Car.createCar(context.applicationContext, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { c, ready ->
                if (!ready) return@createCar
                manager = (c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager).also {
                    it.registerListener(listener)
                    apply(it.currentCarUxRestrictions)
                }
            }
        }.onFailure { Log.w(TAG, "car service unavailable", it) }
    }

    private fun apply(r: CarUxRestrictions?) {
        val required = r?.isRequiresDistractionOptimization == true
        if (required != _requiresDo.value) Log.i(TAG, "requiresDO=$required active=${r?.activeRestrictions}")
        _requiresDo.value = required
    }

    fun release() {
        runCatching { manager?.unregisterListener() }
        car?.disconnect()
    }

    private companion object {
        const val TAG = "Hermes/Drive"
    }
}
