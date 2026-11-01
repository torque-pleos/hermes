package com.zigae.hermes.weather

import com.zigae.hermes.radar.RainLevel
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 격자 한 칸의 1시간 단위 강수 예보 */
data class KmaHourly(
    /** 예보 시각(그 시각부터 1시간) epoch 초 */
    val startEpochSec: Long,
    val rateMmPerHour: Double,
    /** 강수형태 PTY: 0 없음, 1 비, 2 비/눈, 3 눈, 5 빗방울, 6 빗방울눈날림, 7 눈날림 */
    val pty: Int,
    /** 강수확률(%) — 단기예보만 제공 */
    val pop: Int?,
) {
    val level: RainLevel get() = RainLevel.fromRate(rateMmPerHour, pty)
    val isSnow get() = pty == 3 || pty == 7 || pty == 2 || pty == 6
}

/** 기상청 VilageFcstInfoService_2.0 응답(JSON) 해석 */
object KmaParser {
    val KST: ZoneId = ZoneId.of("Asia/Seoul")
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

    /**
     * @throws KmaApiException 결과 코드가 정상(00)이 아닐 때 (03 NO_DATA 포함)
     */
    fun parse(json: String): List<KmaHourly> {
        val root = JSONObject(json)
        // 인증 오류는 HTTP 403 + {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"returnReasonCode":"30",...}}}
        root.optJSONObject("OpenAPI_ServiceResponse")?.optJSONObject("cmmMsgHeader")?.let {
            throw KmaApiException(it.optString("returnReasonCode", "XX"), it.optString("errMsg"))
        }
        val response = root.optJSONObject("response")
            ?: throw KmaApiException("XX", root.toString().take(200))
        val header = response.getJSONObject("header")
        val code = header.getString("resultCode")
        if (code != "00") throw KmaApiException(code, header.optString("resultMsg"))
        val items = response.getJSONObject("body").getJSONObject("items").getJSONArray("item")

        data class Acc(var rate: Double? = null, var pty: Int? = null, var pop: Int? = null)
        val byTime = sortedMapOf<String, Acc>()
        for (i in 0 until items.length()) {
            val it = items.getJSONObject(i)
            val t = it.getString("fcstDate") + it.getString("fcstTime")
            val acc = byTime.getOrPut(t) { Acc() }
            val v = it.get("fcstValue").toString()
            when (it.getString("category")) {
                "RN1", "PCP" -> acc.rate = parseAmount(v)
                "PTY" -> acc.pty = v.toIntOrNull()
                "POP" -> acc.pop = v.toIntOrNull()
            }
        }
        return byTime.mapNotNull { (t, a) ->
            val rate = a.rate ?: return@mapNotNull null
            KmaHourly(
                LocalDateTime.parse(t, stamp).atZone(KST).toEpochSecond(),
                rate, a.pty ?: 0, a.pop,
            )
        }
    }

    /** "강수없음", "1mm 미만", "1.0mm 미만", "2.5mm", "30.0~50.0mm", "50.0mm 이상" → mm/h */
    fun parseAmount(v: String): Double {
        val s = v.trim()
        if (s.isEmpty() || s == "강수없음" || s == "0") return 0.0
        val nums = Regex("""\d+(\.\d+)?""").findAll(s).map { it.value.toDouble() }.toList()
        if (nums.isEmpty()) return 0.0
        return when {
            s.contains("미만") -> nums.first() / 2
            s.contains("~") && nums.size >= 2 -> (nums[0] + nums[1]) / 2
            else -> nums.first()
        }
    }
}

class KmaApiException(val code: String, message: String) : Exception("KMA $code $message")

/**
 * 발표 시각 계산.
 * - 초단기예보: 매시 30분 발표, 45분 이후 조회 가능 → 6시간
 * - 단기예보: 02·05·08·11·14·17·20·23시 발표, 10분 이후 조회 가능 → 약 3일
 */
object KmaBaseTime {
    private val date = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val time = DateTimeFormatter.ofPattern("HHmm")

    fun ultraShort(nowEpochSec: Long): Pair<String, String> {
        var t = LocalDateTime.ofEpochSecond(nowEpochSec, 0, KmaParser.KST.rules.getOffset(java.time.Instant.ofEpochSecond(nowEpochSec)))
        if (t.minute < 45) t = t.minusHours(1)
        t = t.withMinute(30).withSecond(0)
        return t.format(date) to t.format(time)
    }

    fun shortTerm(nowEpochSec: Long): Pair<String, String> {
        val instant = java.time.Instant.ofEpochSecond(nowEpochSec)
        var t = LocalDateTime.ofInstant(instant, KmaParser.KST).minusMinutes(10)
        val hours = listOf(2, 5, 8, 11, 14, 17, 20, 23)
        val h = hours.lastOrNull { it <= t.hour }
        t = if (h == null) t.minusDays(1).withHour(23) else t.withHour(h)
        t = t.withMinute(0).withSecond(0)
        return t.format(date) to t.format(time)
    }
}
