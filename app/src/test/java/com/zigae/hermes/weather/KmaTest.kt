package com.zigae.hermes.weather

import com.zigae.hermes.radar.RainLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class KmaTest {

    @Test
    fun gridMatchesKmaReferenceCells() {
        // 기상청 격자 위경도 자료의 대표 값
        assertEquals(KmaCell(60, 127), KmaGrid.toCell(37.5665, 126.9780)) // 서울 중구
        assertEquals(KmaCell(98, 76), KmaGrid.toCell(35.1796, 129.0756)) // 부산 연제구(시청)
        assertEquals(KmaCell(53, 38), KmaGrid.toCell(33.4996, 126.5312)) // 제주시
    }

    @Test
    fun parsesAmountStrings() {
        assertEquals(0.0, KmaParser.parseAmount("강수없음"), 0.0)
        assertEquals(0.5, KmaParser.parseAmount("1mm 미만"), 1e-9)
        assertEquals(0.5, KmaParser.parseAmount("1.0mm 미만"), 1e-9)
        assertEquals(2.5, KmaParser.parseAmount("2.5mm"), 1e-9)
        assertEquals(40.0, KmaParser.parseAmount("30.0~50.0mm"), 1e-9)
        assertEquals(50.0, KmaParser.parseAmount("50.0mm 이상"), 1e-9)
    }

    @Test
    fun parsesUltraShortResponse() {
        val json = """
        {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
         "body":{"dataType":"JSON","items":{"item":[
          {"baseDate":"20261101","baseTime":"2130","category":"RN1","fcstDate":"20261101","fcstTime":"2200","fcstValue":"강수없음","nx":60,"ny":127},
          {"baseDate":"20261101","baseTime":"2130","category":"PTY","fcstDate":"20261101","fcstTime":"2200","fcstValue":"0","nx":60,"ny":127},
          {"baseDate":"20261101","baseTime":"2130","category":"RN1","fcstDate":"20261101","fcstTime":"2300","fcstValue":"20.0mm","nx":60,"ny":127},
          {"baseDate":"20261101","baseTime":"2130","category":"PTY","fcstDate":"20261101","fcstTime":"2300","fcstValue":"1","nx":60,"ny":127},
          {"baseDate":"20261101","baseTime":"2130","category":"T1H","fcstDate":"20261101","fcstTime":"2300","fcstValue":"12","nx":60,"ny":127}
         ]},"pageNo":1,"numOfRows":1000,"totalCount":5}}}
        """.trimIndent()
        val hours = KmaParser.parse(json)
        assertEquals(2, hours.size)
        assertEquals(ZonedDateTime.of(2026, 11, 1, 22, 0, 0, 0, KmaParser.KST).toEpochSecond(), hours[0].startEpochSec)
        assertEquals(RainLevel.NONE, hours[0].level)
        assertEquals(RainLevel.HEAVY, hours[1].level)
    }

    @Test
    fun errorCodeThrows() {
        val json = """{"response":{"header":{"resultCode":"03","resultMsg":"NO_DATA"}}}"""
        val e = runCatching { KmaParser.parse(json) }.exceptionOrNull()
        assertTrue(e is KmaApiException && e.code == "03")
    }

    @Test
    fun mergePrefersUltraButKeepsShortForUncoveredHours() {
        val h = 3600L
        val short = (21..30).map { KmaHourly(it * h, 1.0, 0, 40) } // 21시~익일 6시
        val ultra = (23..28).map { KmaHourly(it * h, 5.0, 1, null) } // 23시~익일 4시
        val merged = mergeKma(ultra, short)
        assertEquals((21..30).map { it * h }, merged.map { it.startEpochSec })
        assertEquals(1.0, merged.first { it.startEpochSec == 22 * h }.rateMmPerHour, 0.0) // 현재 시간대 = 단기
        assertEquals(5.0, merged.first { it.startEpochSec == 23 * h }.rateMmPerHour, 0.0) // 겹치면 초단기
        assertEquals(1.0, merged.first { it.startEpochSec == 29 * h }.rateMmPerHour, 0.0) // 6시간 이후 = 단기
    }

    @Test
    fun unregisteredKeyResponse() {
        // 실제 응답 (잘못된 키, HTTP 403) — 2026-11-01 확인
        val json = """{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR","returnAuthMsg":"등록되지 않은 서비스키","returnReasonCode":"30"}}}"""
        val e = runCatching { KmaParser.parse(json) }.exceptionOrNull()
        assertTrue(e is KmaApiException && e.code == "30")
    }

    @Test
    fun baseTimes() {
        fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 11, 1, h, m, 0, 0, KmaParser.KST).toEpochSecond()
        assertEquals("20261101" to "2130", KmaBaseTime.ultraShort(at(21, 50)))
        assertEquals("20261101" to "2030", KmaBaseTime.ultraShort(at(21, 40)))
        assertEquals("20261031" to "2330", KmaBaseTime.ultraShort(at(0, 10)))
        assertEquals("20261101" to "2000", KmaBaseTime.shortTerm(at(21, 50)))
        assertEquals("20261101" to "2300", KmaBaseTime.shortTerm(at(23, 10)))
        assertEquals("20261031" to "2300", KmaBaseTime.shortTerm(at(2, 5)))
    }
}
