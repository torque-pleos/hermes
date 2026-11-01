package com.zigae.hermes.fleet

import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FleetFusionTest {
    private val now = 10_000L
    private val daejeon = RoutePoint("대전", 36.35, 127.38, now + 30 * 60)
    private val radarDry = PointForecast.Rain(RainLevel.NONE, 0f, 0.0, 30)
    private val radarLight = PointForecast.Rain(RainLevel.LIGHT, 20f, 0.6, 30)

    private fun report(id: String, lat: Double, lon: Double, level: RainLevel, ageMin: Int = 5) =
        FleetReport(id, lat, lon, now - ageMin * 60, level)

    @Test
    fun twoWetVehiclesUpgradeDryRadar() {
        val reports = listOf(
            report("a", 36.35, 127.38, RainLevel.HEAVY),
            report("b", 36.36, 127.39, RainLevel.MODERATE),
            report("c", 36.34, 127.37, RainLevel.LIGHT),
        )
        val f = FleetForecaster({ radarDry }, reports, now, null).forecast(daejeon)
        assertTrue(f is PointForecast.Fleet)
        // 두 번째로 강한 값(보통 비) — 한 대의 강한 비 관측에 휘둘리지 않는다
        assertEquals(RainLevel.MODERATE, f.rainLevel)
        assertEquals(3, (f as PointForecast.Fleet).evidence.wetVehicles)
    }

    @Test
    fun singleWetVehicleDoesNotChangeForecast() {
        val f = FleetForecaster({ radarDry }, listOf(report("a", 36.35, 127.38, RainLevel.HEAVY)), now, null)
            .forecast(daejeon)
        assertEquals(RainLevel.NONE, f.rainLevel)
    }

    @Test
    fun threeDryVehiclesSuppressLightRadarFalseAlarm() {
        val reports = (1..3).map { report("v$it", 36.35 + it * 0.005, 127.38, RainLevel.NONE) }
        assertEquals(RainLevel.NONE, FleetForecaster({ radarLight }, reports, now, null).forecast(daejeon).rainLevel)
        // 보통 비 이상 레이더는 맑음 관측만으로 낮추지 않는다
        val radarModerate = PointForecast.Rain(RainLevel.MODERATE, 35f, 5.6, 30)
        assertEquals(RainLevel.MODERATE, FleetForecaster({ radarModerate }, reports, now, null).forecast(daejeon).rainLevel)
    }

    @Test
    fun usesLatestReportPerVehicleAndIgnoresStale() {
        val reports = listOf(
            report("a", 36.35, 127.38, RainLevel.HEAVY, ageMin = 15),
            report("a", 36.35, 127.38, RainLevel.NONE, ageMin = 2), // 같은 차 최신 = 맑음
            report("b", 36.35, 127.38, RainLevel.HEAVY, ageMin = 40), // 너무 오래됨
        )
        val ev = FleetFusion.evidence(reports, daejeon, now, null)!!
        assertEquals(1, ev.vehicles)
        assertEquals(0, ev.wetVehicles)
    }

    @Test
    fun advectsReportsWithRainMotion() {
        // 비를 본 차량 2대가 대전 서쪽 30km에 있고 비구름이 동쪽 60km/h → 30분 뒤 대전
        val west = 127.38 - 30_000 / (111_320 * Math.cos(Math.toRadians(36.35)))
        val reports = listOf(report("a", 36.35, west, RainLevel.MODERATE, 0), report("b", 36.351, west, RainLevel.MODERATE, 0))
        val eastward = Wind { _, _ -> 60_000.0 / 3600 to 0.0 }
        assertEquals(RainLevel.MODERATE, FleetForecaster({ radarDry }, reports, now, eastward).forecast(daejeon).rainLevel)
        // 이동을 모르면 30km 떨어진 관측은 반영되지 않는다
        assertEquals(RainLevel.NONE, FleetForecaster({ radarDry }, reports, now, null).forecast(daejeon).rainLevel)
    }

    @Test
    fun beyondSixtyMinutesIgnoresFleet() {
        val far = daejeon.copy(etaEpochSec = now + 90 * 60)
        val reports = (1..3).map { report("v$it", 36.35, 127.38, RainLevel.HEAVY) }
        assertNull(FleetFusion.evidence(reports, far, now, null))
    }
}
