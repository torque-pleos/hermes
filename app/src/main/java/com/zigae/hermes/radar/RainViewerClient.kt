package com.zigae.hermes.radar

import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** RainViewer 공개 API (개인·교육용 무료, 100 req/min/IP, 최대 zoom 7, 과거 2시간 10분 간격). */
class RainViewerClient {

    data class RadarIndex(val host: String, val frames: List<FrameRef>)
    data class FrameRef(val timeEpochSec: Long, val path: String)

    fun fetchIndex(): RadarIndex {
        val json = JSONObject(get(INDEX_URL).decodeToString())
        val host = json.getString("host")
        val past = json.getJSONObject("radar").getJSONArray("past")
        val frames = (0 until past.length()).map {
            val f = past.getJSONObject(it)
            FrameRef(f.getLong("time"), f.getString("path"))
        }
        return RadarIndex(host, frames.sortedBy { it.timeEpochSec })
    }

    /**
     * 256px, 스무딩 없음, 눈 표시 → ARGB 픽셀.
     * 색상표 파라미터(2=Universal Blue)는 현재 서버가 무시하지만 실제 응답과 맞춰 둔다.
     */
    fun fetchTile(host: String, path: String, zoom: Int, x: Int, y: Int): IntArray {
        val bytes = get("$host$path/256/$zoom/$x/$y/2/0_1.png")
        val opts = BitmapFactory.Options().apply { inPremultiplied = false }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: throw IOException("tile decode failed $zoom/$x/$y")
        try {
            if (bmp.width != WebMercator.TILE_SIZE || bmp.height != WebMercator.TILE_SIZE) {
                throw IOException("unexpected tile size ${bmp.width}x${bmp.height}")
            }
            val px = IntArray(bmp.width * bmp.height)
            bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            return px
        } finally {
            bmp.recycle()
        }
    }

    private fun get(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "Hermes/0.1 (Pleos Connect)")
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode} $url")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val INDEX_URL = "https://api.rainviewer.com/public/weather-maps.json"
    }
}
