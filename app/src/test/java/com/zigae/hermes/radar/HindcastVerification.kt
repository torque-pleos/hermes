package com.zigae.hermes.radar

import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 실제 RainViewer 과거 프레임으로 외삽 예측 정확도를 측정하는 검증(네트워크 필요).
 *
 *   ./gradlew :app:testDebugUnitTest --tests '*HindcastVerification*' -Phindcast
 *
 * 기준 시각 t의 직전 4프레임으로 이동 벡터를 추정하고 t+10..90분을 예측해 실제 관측과 비교한다.
 * 비교 대상: 지속성 예측(비구름이 움직이지 않는다고 가정 = 이동 벡터 0).
 * 평가는 앱과 같은 방식(Nowcaster.forecast의 지점 예측)으로 한반도 영역 격자점에서 한다.
 */
class HindcastVerification {

    private val range = TileRange(zoom = 7, minX = 108, minY = 48, maxX = 110, maxY = 51)
    private val cacheDir = File(System.getProperty("hermes.cacheDir") ?: "build/rainviewer-cache")

    @Test
    fun hindcast() {
        assumeTrue("enable with -Phindcast", System.getProperty("hermes.hindcast") == "true")
        cacheDir.mkdirs()

        val index = JSONObject(URL("https://api.rainviewer.com/public/weather-maps.json").readText())
        val host = index.getString("host")
        val past = index.getJSONObject("radar").getJSONArray("past")
        val refs = (0 until past.length()).map { past.getJSONObject(it) }
            .map { it.getLong("time") to it.getString("path") }.sortedBy { it.first }

        val frames = refs.map { (time, path) ->
            val tiles = range.tiles().associateWith { (x, y) -> tile(host, path, time, x, y) }
            RadarMosaic.build(time, range, tiles)
        }
        val report = StringBuilder()
        fun out(s: String) { println(s); report.appendLine(s) }

        out("# Hermes 레이더 외삽 예측 검증 (hindcast)")
        out("")
        out("- 프레임: ${frames.size}개 (${fmt(frames.first().timeEpochSec)} ~ ${fmt(frames.last().timeEpochSec)} UTC, 10분 간격)")
        out("- 영역: z7 타일 x ${range.minX}..${range.maxX}, y ${range.minY}..${range.maxY} (한반도·주변 해역)")
        val echoShare = frames.map { f -> f.dbz.count { it >= RainLevel.MIN_RAIN_DBZ } * 100.0 / f.dbz.size }
        out("- 강수 에코(≥15dBZ) 면적 비율: 평균 %.1f%% (최소 %.1f%%, 최대 %.1f%%)".format(echoShare.average(), echoShare.min(), echoShare.max()))
        out("- 평가 지점: 8px(≈8km) 간격 격자, 사건 = 해당 강도 이상")
        out("- 지표: CSI = 적중/(적중+놓침+오경보), POD = 적중/(적중+놓침), FAR = 오경보/(적중+오경보)")
        out("")

        for (threshold in listOf(RainLevel.LIGHT, RainLevel.MODERATE)) {
            out("## 사건: ${threshold.label} 이상")
            out("")
            out("| 리드 | 사례 | 이동벡터 CSI | 지속성 CSI | 이동벡터 POD | 이동벡터 FAR | 지속성 POD | 지속성 FAR | 관측 사건 수 |")
            out("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
            for (k in 1..9) {
                val adv = Score()
                val per = Score()
                var cases = 0
                for (i in 3 until frames.size - k) {
                    val history = frames.subList(i - 3, i + 1)
                    val motion = MotionEstimator.estimate(history)
                    val base = frames[i]
                    val truth = frames[i + k]
                    val castAdv = Nowcaster(base, motion)
                    val castPer = Nowcaster(base, Motion.NONE)
                    cases++
                    for (gy in 16 until base.height - 16 step 8) for (gx in 16 until base.width - 16 step 8) {
                        val lat = WebMercator.lat((base.originY + gy).toDouble(), base.zoom)
                        val lon = WebMercator.lon((base.originX + gx).toDouble(), base.zoom)
                        val observed = RainLevel.fromDbz(truth[gx, gy]) >= threshold
                        adv.add(levelOf(castAdv.forecast(lat, lon, truth.timeEpochSec)) >= threshold, observed)
                        per.add(levelOf(castPer.forecast(lat, lon, truth.timeEpochSec)) >= threshold, observed)
                    }
                }
                if (cases == 0) continue
                out("| ${k * 10}분 | $cases | %.3f | %.3f | %.3f | %.3f | %.3f | %.3f | ${adv.hit + adv.miss} |".format(
                    adv.csi, per.csi, adv.pod, adv.far, per.pod, per.far))
            }
            out("")
        }

        out("## 추정 이동 벡터 (직전 4프레임 기준)")
        out("")
        out("| 기준 시각(UTC) | 방향 | 속도 | 신뢰도 |")
        out("| --- | --- | --- | --- |")
        for (i in 3 until frames.size) {
            val m = MotionEstimator.estimate(frames.subList(i - 3, i + 1))
            out("| ${fmt(frames[i].timeEpochSec)} | %.0f° | %.0f km/h | %.2f |".format(m.headingDeg(), m.speedKmh(36.5, 7), m.confidence))
        }

        File("build/reports").mkdirs()
        File("build/reports/hindcast.md").writeText(report.toString())
    }

    private fun levelOf(f: PointForecast): RainLevel = when (f) {
        is PointForecast.Rain -> f.level
        else -> RainLevel.NONE
    }

    private class Score {
        var hit = 0; var miss = 0; var falseAlarm = 0
        fun add(forecast: Boolean, observed: Boolean) {
            when {
                forecast && observed -> hit++
                !forecast && observed -> miss++
                forecast && !observed -> falseAlarm++
            }
        }
        val csi get() = hit.toDouble() / (hit + miss + falseAlarm).coerceAtLeast(1)
        val pod get() = hit.toDouble() / (hit + miss).coerceAtLeast(1)
        val far get() = falseAlarm.toDouble() / (hit + falseAlarm).coerceAtLeast(1)
    }

    /** 디스크 캐시 + 요청 간격 0.7초 (RainViewer 100 req/min/IP 제한 준수) */
    private fun tile(host: String, path: String, time: Long, x: Int, y: Int): IntArray {
        val file = File(cacheDir, "$time-7-$x-$y.png")
        if (!file.exists()) {
            Thread.sleep(700)
            val conn = URL("$host$path/256/7/$x/$y/2/0_1.png").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Hermes-hindcast/0.1")
            check(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
            file.writeBytes(conn.inputStream.use { it.readBytes() })
        }
        // Android 단위 테스트는 android.jar로 컴파일되어 javax.imageio가 보이지 않으므로 리플렉션으로 호출(실행 JDK에는 있음)
        val img = Class.forName("javax.imageio.ImageIO").getMethod("read", File::class.java).invoke(null, file)
        val cls = img.javaClass
        val w = cls.getMethod("getWidth").invoke(img) as Int
        val h = cls.getMethod("getHeight").invoke(img) as Int
        val get = cls.getMethod(
            "getRGB", Int::class.java, Int::class.java, Int::class.java, Int::class.java,
            IntArray::class.java, Int::class.java, Int::class.java,
        )
        return get.invoke(img, 0, 0, w, h, null, 0, w) as IntArray
    }

    private fun fmt(epoch: Long) = java.time.Instant.ofEpochSecond(epoch).toString().substring(5, 16).replace('T', ' ')
}
