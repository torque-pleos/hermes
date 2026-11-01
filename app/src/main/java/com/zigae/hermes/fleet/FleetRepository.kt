package com.zigae.hermes.fleet

import android.content.Context
import android.util.Log
import com.zigae.hermes.radar.RainLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToLong

data class FleetState(
    val configured: Boolean,
    /** 위치·와이퍼 공유 동의 */
    val consent: Boolean,
    val reports: List<FleetReport> = emptyList(),
    val lastUploadEpochSec: Long = 0,
    val lastFetchEpochSec: Long = 0,
    val error: String? = null,
)

/**
 * 차량 간 와이퍼 관측 공유 (Supabase PostgREST).
 * - 동의한 경우에만 업로드. 차량 ID는 설치마다 만든 무작위 UUID, 좌표는 약 500m 단위로 반올림.
 * - 공모전 시연용: publishable 키를 앱에 넣고 RLS로 최근 30분 조회·현재 시각 삽입만 허용한다.
 */
class FleetRepository(context: Context, private val baseUrl: String, private val apiKey: String) {

    private val prefs = context.applicationContext.getSharedPreferences("fleet", Context.MODE_PRIVATE)
    val vehicleId: String = prefs.getString(KEY_VEHICLE, null)
        ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_VEHICLE, it).apply() }

    private val _state = MutableStateFlow(
        FleetState(configured = baseUrl.isNotBlank() && apiKey.isNotBlank(), consent = prefs.getBoolean(KEY_CONSENT, false)),
    )
    val state: StateFlow<FleetState> = _state.asStateFlow()

    fun setConsent(on: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT, on).apply()
        _state.update { it.copy(consent = on) }
    }

    /** 내 차 관측 올리기. 동의·설정이 없거나 강도를 모르면 하지 않는다 */
    suspend fun upload(lat: Double, lon: Double, level: RainLevel?, wiper: String?) {
        val s = _state.value
        if (!s.configured || !s.consent || level == null) return
        val body = JSONObject()
            .put("vehicle_id", vehicleId)
            .put("lat", round(lat))
            .put("lon", round(lon))
            .put("level", level.ordinal)
            .put("wiper", wiper)
        runCatching { request("POST", "/rest/v1/wiper_reports", body.toString(), prefer = "return=minimal") }
            .onSuccess { _state.update { it.copy(lastUploadEpochSec = now(), error = null) } }
            .onFailure { e -> fail("upload", e) }
    }

    /** 영역 안 최근 관측 받기 (내 차 제외 — 내 차는 로컬 관측을 쓴다) */
    suspend fun fetch(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double) {
        if (!_state.value.configured) return
        val since = Instant.ofEpochSecond(now() - FleetFusion.MAX_AGE_SEC).toString()
        val q = "select=vehicle_id,lat,lon,level,demo,observed_at" +
            "&observed_at=gte.${enc(since)}" +
            "&lat=gte.$minLat&lat=lte.$maxLat&lon=gte.$minLon&lon=lte.$maxLon" +
            "&vehicle_id=neq.$vehicleId&order=observed_at.desc&limit=1000"
        runCatching { JSONArray(request("GET", "/rest/v1/wiper_reports?$q", null)) }
            .onSuccess { arr ->
                val reports = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    FleetReport(
                        vehicleId = o.getString("vehicle_id"),
                        lat = o.getDouble("lat"),
                        lon = o.getDouble("lon"),
                        observedAtEpochSec = Instant.parse(normalize(o.getString("observed_at"))).epochSecond,
                        level = RainLevel.entries[o.getInt("level").coerceIn(0, RainLevel.entries.size - 1)],
                        demo = o.optBoolean("demo"),
                    )
                }
                _state.update { it.copy(reports = reports, lastFetchEpochSec = now(), error = null) }
            }
            .onFailure { e -> fail("fetch", e) }
    }

    /** 시연용: 두 지점 사이에 비를 보는 가상 차량 관측을 서버에 만든다 */
    suspend fun seedDemo(lat1: Double, lon1: Double, lat2: Double, lon2: Double, level: RainLevel, vehicles: Int) {
        if (!_state.value.configured) return
        val body = JSONObject()
            .put("lat1", lat1).put("lon1", lon1).put("lat2", lat2).put("lon2", lon2)
            .put("lvl", level.ordinal).put("n", vehicles)
        runCatching { request("POST", "/rest/v1/rpc/seed_demo_fleet", body.toString()) }
            .onFailure { e -> fail("seed", e) }
    }

    private suspend fun request(method: String, path: String, body: String?, prefer: String? = null): String =
        withContext(Dispatchers.IO) {
            val conn = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("apikey", apiKey)
            conn.setRequestProperty("Content-Type", "application/json")
            prefer?.let { conn.setRequestProperty("Prefer", it) }
            try {
                if (body != null) {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray()) }
                }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.use { it.readBytes().decodeToString() }.orEmpty()
                if (code !in 200..299) throw IOException("HTTP $code ${text.take(160)}")
                text
            } finally {
                conn.disconnect()
            }
        }

    private fun fail(what: String, e: Throwable) {
        Log.w(TAG, "fleet $what failed", e)
        val msg = when (e) {
            is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
                "네트워크 연결 없음"
            else -> "서버 오류 (${e.message?.take(60)})"
        }
        _state.update { it.copy(error = msg) }
    }

    /** 약 500m 단위 반올림(위치 노출 최소화) */
    private fun round(v: Double) = (v * 200).roundToLong() / 200.0

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** PostgREST timestamptz("2026-11-01T13:55:01.123+00:00") → Instant 파싱 가능 형식 */
    private fun normalize(ts: String) = ts.replace(" ", "T").let { if (it.endsWith("+00:00")) it.dropLast(6) + "Z" else it }

    private fun now() = System.currentTimeMillis() / 1000

    private companion object {
        const val TAG = "Hermes/Fleet"
        const val KEY_VEHICLE = "vehicle_id"
        const val KEY_CONSENT = "consent"
    }
}
