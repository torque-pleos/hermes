package com.zigae.hermes.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoTest {
    private val here = LatLon(37.41272, 127.09468) // 성남 수정구
    private val daejeonDunsan = LatLon(36.3561, 127.3907)
    private val busanDunsan = LatLon(35.2100, 129.0900) // 가상의 다른 도시 동명 후보
    private val incheonSeogu = LatLon(37.5450, 126.6760)

    @Test
    fun distanceSeoulBusanAboutRight() {
        val d = GeoMath.distanceMeters(here, LatLon(35.0765, 128.8203))
        assertEquals(305_000.0, d, 5_000.0)
    }

    @Test
    fun interpolationEndpointsAndMidpoint() {
        val b = LatLon(35.0765, 128.8203)
        assertEquals(here.lat, GeoMath.interpolate(here, b, 0.0).lat, 1e-9)
        assertEquals(b.lon, GeoMath.interpolate(here, b, 1.0).lon, 1e-9)
        val mid = GeoMath.interpolate(here, b, 0.5)
        assertEquals(GeoMath.distanceMeters(here, mid), GeoMath.distanceMeters(mid, b), 1.0)
    }

    @Test
    fun picksCandidateConsistentWithRemainingRoadDistance() {
        // 내비 남은 거리 141km → 대전 둔산동(직선 ≈120km)이 맞고, 인천(≈40km)·부산(≈280km)은 아니다
        val picked = CandidatePicker.pick(listOf(incheonSeogu, busanDunsan, daejeonDunsan), here, 141_000)
        assertEquals(daejeonDunsan, picked)
    }

    @Test
    fun rejectsWhenNoCandidateFits() {
        assertNull(CandidatePicker.pick(listOf(incheonSeogu), here, 141_000))
    }

    @Test
    fun withoutContextTakesFirst() {
        assertEquals(incheonSeogu, CandidatePicker.pick(listOf(incheonSeogu, daejeonDunsan), null, 0))
    }
}
