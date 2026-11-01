package com.zigae.hermes.radar

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class RadarState(
    val loading: Boolean = false,
    val nowcaster: Nowcaster? = null,
    val range: TileRange? = null,
    /** 실제로 받은 타일 수 (range 안에서 경로에 필요한 것만) */
    val tileCount: Int = 0,
    val framesUsed: Int = 0,
    val fetchedAtEpochSec: Long = 0,
    val error: String? = null,
)

/**
 * 관심 지점(현재 위치·경유지·목적지)을 덮는 레이더 타일을 주기적으로 받아 이동 벡터와 외삽 예측기를 만든다.
 * 같은 프레임·타일은 캐시해서 새 프레임 타일만 받는다.
 */
class RadarRepository(
    private val scope: CoroutineScope,
    /** 경로 표본 지점(도착 예정 시각 포함) */
    private val pointsProvider: () -> List<RoutePoint>,
    private val client: RainViewerClient = RainViewerClient(),
) {
    private val _state = MutableStateFlow(RadarState())
    val state: StateFlow<RadarState> = _state.asStateFlow()

    /** key: "path/z/x/y" */
    private val tileCache = LruCache<String, IntArray>(MAX_CACHED_TILES)
    private val mutex = Mutex()
    /** RainViewer 요청 동시성 제한 */
    private val io = Dispatchers.IO.limitedParallelism(PARALLEL_DOWNLOADS)
    private var job: Job? = null
    private var lastTiles: Set<Pair<Int, Int>> = emptySet()
    private var lastRefreshMs = 0L

    fun start() {
        job = scope.launch {
            while (isActive) {
                val tiles = currentTiles()
                val stale = System.currentTimeMillis() - lastRefreshMs > REFRESH_INTERVAL_MS
                // 경로가 바뀌어 새 타일이 필요해졌을 때도 갱신 (줄어든 경우는 그대로 사용)
                if (tiles.isNotEmpty() && (stale || !lastTiles.containsAll(tiles))) refresh(tiles)
                delay(if (tiles.isEmpty()) WAIT_LOCATION_MS else CHECK_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private fun userMessage(e: Exception): String = when (e) {
        is java.net.UnknownHostException, is java.net.ConnectException, is java.net.SocketTimeoutException ->
            "네트워크 연결 없음 · 1분 후 다시 시도"
        is java.io.IOException -> "레이더 서버 응답 오류 · 1분 후 다시 시도"
        else -> "레이더 처리 오류 (${e.javaClass.simpleName})"
    }

    private fun currentTiles(): Set<Pair<Int, Int>> {
        val now = System.currentTimeMillis() / 1000
        val needs = pointsProvider().map {
            RouteTiles.Need(it.lat, it.lon, ((it.etaEpochSec - now) / 60).toInt() + RADAR_LATENCY_MIN)
        }
        return RouteTiles.select(needs, ZOOM, Nowcaster.DEFAULT_HORIZON_MIN, MAX_TILES)
    }

    private suspend fun refresh(tiles: Set<Pair<Int, Int>>) = mutex.withLock {
        _state.update { it.copy(loading = true) }
        try {
            val result = load(tiles)
            lastTiles = tiles
            lastRefreshMs = System.currentTimeMillis()
            _state.value = result
            val m = result.nowcaster?.motion
            Log.i(TAG, "radar ok frames=${result.framesUsed} tiles=${tiles.size}/${result.range?.count} motion=$m")
        } catch (e: Exception) {
            Log.w(TAG, "radar refresh failed", e)
            lastRefreshMs = System.currentTimeMillis() - REFRESH_INTERVAL_MS + RETRY_DELAY_MS
            // 이미 받은 예측기는 유지하고 오류만 표시한다 (화면에 관측 시각이 함께 보이므로 오래된 정도를 알 수 있음)
            _state.update { it.copy(loading = false, error = userMessage(e)) }
        }
    }

    private suspend fun load(tiles: Set<Pair<Int, Int>>): RadarState = coroutineScope {
        val range = RouteTiles.bounds(ZOOM, tiles)!!
        val index = withContext(io) { client.fetchIndex() }
        val refs = index.frames.takeLast(FRAMES_FOR_MOTION)
        require(refs.isNotEmpty()) { "레이더 프레임 없음" }
        // 프레임 × 타일을 병렬로 받는다 (캐시에 있으면 재사용)
        val pixels = refs.flatMap { ref -> tiles.map { ref to it } }
            .map { (ref, xy) ->
                async(io) {
                    val key = "${ref.path}/${range.zoom}/${xy.first}/${xy.second}"
                    val px = tileCache.get(key)
                        ?: client.fetchTile(index.host, ref.path, range.zoom, xy.first, xy.second)
                            .also { tileCache.put(key, it) }
                    Triple(ref, xy, px)
                }
            }.awaitAll()
        val (frames, motion) = withContext(Dispatchers.Default) {
            val frames = refs.map { ref ->
                RadarMosaic.build(
                    ref.timeEpochSec, range,
                    pixels.filter { it.first == ref }.associate { it.second to it.third },
                )
            }
            frames to MotionEstimator.estimate(frames)
        }
        // 블록별 벡터장 (hindcast상 같은 반경의 전역 벡터 대비 CSI 동등 이상)
        val field = withContext(Dispatchers.Default) { MotionEstimator.estimateField(frames, motion) }
        RadarState(
            loading = false,
            nowcaster = Nowcaster(frames.last(), motion, field = field),
            range = range,
            tileCount = tiles.size,
            framesUsed = frames.size,
            fetchedAtEpochSec = System.currentTimeMillis() / 1000,
        )
    }

    private companion object {
        const val TAG = "Hermes/Radar"
        const val ZOOM = 7 // RainViewer 최대 zoom
        const val MAX_TILES = 24
        /** 최신 레이더 프레임은 보통 10~20분 전 관측이라 외삽 시간에 더한다 */
        const val RADAR_LATENCY_MIN = 15
        /** 이동 추정용 최근 프레임 수 (10분 간격 → 30분 구간) */
        const val FRAMES_FOR_MOTION = 4
        const val MAX_CACHED_TILES = FRAMES_FOR_MOTION * MAX_TILES + MAX_TILES
        const val REFRESH_INTERVAL_MS = 5 * 60_000L
        const val CHECK_INTERVAL_MS = 30_000L
        const val WAIT_LOCATION_MS = 2_000L
        const val RETRY_DELAY_MS = 60_000L
        const val PARALLEL_DOWNLOADS = 8
    }
}
