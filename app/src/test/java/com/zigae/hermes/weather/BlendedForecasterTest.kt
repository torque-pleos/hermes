package com.zigae.hermes.weather

import com.zigae.hermes.radar.Motion
import com.zigae.hermes.radar.Nowcaster
import com.zigae.hermes.radar.PointForecast
import com.zigae.hermes.radar.RadarFrame
import com.zigae.hermes.radar.RainLevel
import com.zigae.hermes.radar.RoutePoint
import com.zigae.hermes.radar.WebMercator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlendedForecasterTest {
    private val now = 1_000_000L
    // 전 영역 25dBZ(약한 비) 레이더
    private val frame = RadarFrame(now - 600, 7, 109 * 256, 49 * 256, 256, 256, FloatArray(256 * 256) { 25f })
    private val lat = WebMercator.lat(49 * 256 + 128.0, 7)
    private val lon = WebMercator.lon(109 * 256 + 128.0, 7)
    private val heavyKma = listOf(
        KmaHourly(now + 3000, 0.0, 0, 10),
        KmaHourly(now + 3000 + 3600, 20.0, 1, 80), // 강한 비
    )

    private fun blended(kma: List<KmaHourly>?) =
        BlendedForecaster(Nowcaster(frame, Motion.NONE), { _, _ -> kma }, now)

    @Test
    fun usesRadarWithinSixtyMinutes() {
        val f = blended(heavyKma).forecast(RoutePoint("p", lat, lon, now + 30 * 60))
        assertTrue(f is PointForecast.Rain && f.level == RainLevel.LIGHT)
    }

    @Test
    fun usesKmaHourSlotBeyondSixtyMinutes() {
        val f = blended(heavyKma).forecast(RoutePoint("p", lat, lon, now + 3000 + 3600 + 100))
        assertTrue(f is PointForecast.Kma && f.level == RainLevel.HEAVY && f.pop == 80)
        assertEquals(now + 3000 + 3600, (f as PointForecast.Kma).hourStartEpochSec)
    }

    @Test
    fun fallsBackToRadarWhenKmaMissing() {
        val f = blended(null).forecast(RoutePoint("p", lat, lon, now + 3 * 3600))
        assertTrue(f is PointForecast.BeyondHorizon && f.observedNow == RainLevel.LIGHT)
    }

    @Test
    fun usesKmaWhenOutsideRadarCoverage() {
        val f = blended(heavyKma).forecast(RoutePoint("p", 33.0, 140.0, now + 10 * 60).copy(etaEpochSec = now + 3000 + 3700))
        assertTrue(f is PointForecast.Kma)
    }
}
