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
    private val pointsProvider: () -> List<Pair<Double, Double>>,
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
    private var lastRange: TileRange? = null
    private var lastRefreshMs = 0L

    fun start() {
        job = scope.launch {
            while (isActive) {
                val range = currentRange()
                val stale = System.currentTimeMillis() - lastRefreshMs > REFRESH_INTERVAL_MS
                if (range != null && (stale || range != lastRange)) refresh(range)
                delay(if (range == null) WAIT_LOCATION_MS else CHECK_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private fun currentRange(): TileRange? =
        TileRange.covering(pointsProvider(), ZOOM, MARGIN_PX, MAX_TILES)

    private suspend fun refresh(range: TileRange) = mutex.withLock {
        _state.update { it.copy(loading = true) }
        try {
            val result = load(range)
            lastRange = range
            lastRefreshMs = System.currentTimeMillis()
            _state.value = result
            val m = result.nowcaster?.motion
            Log.i(TAG, "radar ok frames=${result.framesUsed} tiles=${range.count} motion=$m")
        } catch (e: Exception) {
            Log.w(TAG, "radar refresh failed", e)
            lastRefreshMs = System.currentTimeMillis() - REFRESH_INTERVAL_MS + RETRY_DELAY_MS
            _state.update { it.copy(loading = false, error = e.message ?: e.javaClass.simpleName) }
        }
    }

    private suspend fun load(range: TileRange): RadarState = coroutineScope {
        val index = withContext(io) { client.fetchIndex() }
        val refs = index.frames.takeLast(FRAMES_FOR_MOTION)
        require(refs.isNotEmpty()) { "레이더 프레임 없음" }
        // 프레임 × 타일을 병렬로 받는다 (캐시에 있으면 재사용)
        val pixels = refs.flatMap { ref -> range.tiles().map { ref to it } }
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
        RadarState(
            loading = false,
            nowcaster = Nowcaster(frames.last(), motion),
            range = range,
            framesUsed = frames.size,
            fetchedAtEpochSec = System.currentTimeMillis() / 1000,
        )
    }

    private companion object {
        const val TAG = "Hermes/Radar"
        const val ZOOM = 7 // RainViewer 최대 zoom
        /** 90분 × 최대 100km/h ≈ 150km ≈ 150px(z7, 중위도) */
        const val MARGIN_PX = 150
        const val MAX_TILES = 16
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
