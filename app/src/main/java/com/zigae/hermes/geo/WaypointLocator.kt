package com.zigae.hermes.geo

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 경유지 좌표 보완. 에뮬레이터 NaviHelper는 WaypointInfo 좌표를 0.0으로 주므로
 * (1) 앱이 직접 요청한 좌표 (2) 저장된 이전 결과 (3) 주소 지오코딩 순으로 찾는다. 결과는 기기에 저장한다.
 *
 * 지오코딩은 OpenStreetMap Nominatim(키 불필요, 초당 1건 제한)을 쓴다.
 * 상용 전환 시 VWorld/카카오 로컬 API로 교체한다.
 */
class WaypointLocator(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("waypoint_coords", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var lastRequestMs = 0L
    /** 실패한 키는 이번 실행 동안 다시 묻지 않는다 */
    private val failed = mutableSetOf<String>()

    /** [address]가 null이면 이름만으로 기억한다(앱이 직접 요청한 경유지 — 내비가 돌려주는 주소가 요청과 다름). */
    fun remember(name: String, address: String?, at: LatLon) {
        prefs.edit().putString(key(name, address), "${at.lat},${at.lon}").apply()
    }

    fun cached(name: String, address: String): LatLon? =
        (prefs.getString(key(name, address), null) ?: prefs.getString(key(name, null), null))
            ?.split(",")?.let { LatLon(it[0].toDouble(), it[1].toDouble()) }

    suspend fun locate(name: String, address: String, here: LatLon?, remainDistanceM: Int): LatLon? {
        cached(name, address)?.let { return it }
        val k = key(name, address)
        if (k in failed) return null
        return mutex.withLock {
            cached(name, address)?.let { return@withLock it }
            val queries = listOf(address, "$name $address").map { it.trim() }.filter { it.isNotBlank() }.distinct()
            for (q in queries) {
                val candidates = runCatching { search(q) }
                    .onFailure { Log.w(TAG, "geocode failed q=$q", it) }
                    .getOrNull() ?: continue
                val picked = CandidatePicker.pick(candidates, here, remainDistanceM)
                Log.i(TAG, "geocode q=$q candidates=${candidates.size} picked=$picked")
                if (picked != null) {
                    remember(name, address, picked)
                    return@withLock picked
                }
            }
            failed += k
            null
        }
    }

    private suspend fun search(query: String): List<LatLon> = withContext(Dispatchers.IO) {
        val wait = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastRequestMs)
        if (wait > 0) delay(wait)
        lastRequestMs = System.currentTimeMillis()
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&countrycodes=kr&limit=5" +
            "&accept-language=ko&q=" + URLEncoder.encode(query, "UTF-8")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "Hermes/0.1 (Pleos Connect freight app)")
        try {
            check(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
            val arr = JSONArray(conn.inputStream.use { it.readBytes().decodeToString() })
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                LatLon(o.getString("lat").toDouble(), o.getString("lon").toDouble())
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun key(name: String, address: String?) = "$name|${address.orEmpty()}"

    private companion object {
        const val TAG = "Hermes/Geo"
        const val MIN_INTERVAL_MS = 1_100L
    }
}
