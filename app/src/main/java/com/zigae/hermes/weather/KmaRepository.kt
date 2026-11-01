package com.zigae.hermes.weather

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class KmaState(
    val enabled: Boolean,
    val cells: Int = 0,
    val updatedAtEpochSec: Long = 0,
    val error: String? = null,
)

/**
 * 기상청 동네예보(VilageFcstInfoService_2.0) 격자별 캐시.
 * 초단기예보는 매시 발표(6시간), 단기예보는 3시간마다 발표(약 3일). 발표 시각이 바뀔 때만 다시 받는다.
 *
 * ⚠️ 서비스 키가 APK에 들어간다. 공모전 시연용이며, 상용은 서버가 격자 단위로 받아 캐시·중계해야 한다
 * (차량 수와 무관하게 호출 수가 격자 수에 비례, 키 노출 방지).
 */
class KmaRepository(private val serviceKey: String) {

    private data class Cached(val base: Pair<String, String>, val hours: List<KmaHourly>)

    private val ultra = ConcurrentHashMap<KmaCell, Cached>()
    private val short = ConcurrentHashMap<KmaCell, Cached>()
    private val mutex = Mutex()
    private var lastLoggedCells = -1
    /** (작업, 격자) → 다시 시도할 수 있는 시각. 실패한 격자를 매분 다시 부르지 않기 위함 */
    private val retryAfter = ConcurrentHashMap<Pair<String, KmaCell>, Long>()
    private val io = Dispatchers.IO.limitedParallelism(PARALLEL)

    private val _state = MutableStateFlow(KmaState(enabled = serviceKey.isNotBlank()))
    val state: StateFlow<KmaState> = _state.asStateFlow()

    /** 지점의 1시간 단위 예보 (초단기 우선, 이후 단기). 아직 없으면 null */
    fun hourly(lat: Double, lon: Double): List<KmaHourly>? {
        val cell = KmaGrid.toCell(lat, lon)
        val u = ultra[cell]?.hours.orEmpty()
        val s = short[cell]?.hours.orEmpty()
        if (u.isEmpty() && s.isEmpty()) return null
        return mergeKma(u, s)
    }

    suspend fun refresh(points: List<Pair<Double, Double>>) {
        if (serviceKey.isBlank()) return
        mutex.withLock {
            val now = System.currentTimeMillis() / 1000
            val cells = points.map { (lat, lon) -> KmaGrid.toCell(lat, lon) }.distinct()
            val uBase = KmaBaseTime.ultraShort(now)
            val sBase = KmaBaseTime.shortTerm(now)
            val errors = mutableListOf<String>()
            coroutineScope {
                cells.flatMap { cell ->
                    listOfNotNull(
                        if (ultra[cell]?.base != uBase) Triple(ULTRA, cell, uBase) else null,
                        if (short[cell]?.base != sBase) Triple(SHORT, cell, sBase) else null,
                    ).filter { (op, c, _) -> (retryAfter[op to c] ?: 0) <= now }
                }.map { (op, cell, base) ->
                    async(io) {
                        runCatching { fetch(op, cell, base) }
                            .onSuccess {
                                (if (op == ULTRA) ultra else short)[cell] = Cached(base, it)
                                retryAfter.remove(op to cell)
                            }
                            .onFailure { e ->
                                val keyProblem = e is KmaApiException && e.code in setOf("20", "30", "31", "32")
                                retryAfter[op to cell] = now + if (keyProblem) KEY_ERROR_BACKOFF_SEC else ERROR_BACKOFF_SEC
                                Log.w(TAG, "kma $op $cell failed", e)
                                synchronized(errors) { errors += userMessage(e) }
                            }
                    }
                }.awaitAll()
            }
            _state.value = KmaState(
                enabled = true,
                cells = cells.size,
                updatedAtEpochSec = now,
                // 이번 회차에 시도하지 않은(재시도 대기) 격자의 이전 오류는 유지
                error = errors.distinct().firstOrNull() ?: _state.value.error.takeIf { retryAfter.isNotEmpty() },
            )
            if (errors.isNotEmpty() || cells.size != lastLoggedCells) Log.i(TAG, "kma cells=${cells.size} errors=${errors.size}")
            lastLoggedCells = cells.size
        }
    }

    private suspend fun fetch(op: String, cell: KmaCell, base: Pair<String, String>): List<KmaHourly> =
        withContext(io) {
            val url = "$BASE_URL/$op?serviceKey=${URLEncoder.encode(serviceKey, "UTF-8")}" +
                "&pageNo=1&numOfRows=1000&dataType=JSON" +
                "&base_date=${base.first}&base_time=${base.second}&nx=${cell.nx}&ny=${cell.ny}"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            try {
                val ok = conn.responseCode == 200
                val body = (if (ok) conn.inputStream else conn.errorStream)?.use { it.readBytes().decodeToString() }
                    ?: throw IOException("HTTP ${conn.responseCode}")
                // 오류 응답도 JSON 본문에 사유 코드가 있으므로 먼저 해석을 시도한다
                runCatching { KmaParser.parse(body) }.getOrElse { e ->
                    if (e is KmaApiException || ok) throw e else throw IOException("HTTP ${conn.responseCode}")
                }
            } finally {
                conn.disconnect()
            }
        }

    private fun userMessage(e: Throwable): String = when (e) {
        is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
            "네트워크 연결 없음"
        is KmaApiException -> when (e.code) {
            "03" -> "기상청 데이터 없음(발표 전)"
            "30" -> "기상청 키 미등록"
            "22" -> "기상청 일일 호출 한도 초과"
            else -> "기상청 오류 ${e.code}"
        }
        else -> "기상청 응답 오류 (${e.javaClass.simpleName})"
    }

    private companion object {
        const val TAG = "Hermes/KMA"
        const val BASE_URL = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0"
        const val ULTRA = "getUltraSrtFcst"
        const val SHORT = "getVilageFcst"
        const val PARALLEL = 4
        const val ERROR_BACKOFF_SEC = 10 * 60L
        const val KEY_ERROR_BACKOFF_SEC = 60 * 60L
    }
}
