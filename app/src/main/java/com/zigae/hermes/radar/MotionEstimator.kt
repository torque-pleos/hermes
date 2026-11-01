package com.zigae.hermes.radar

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** 강수 영역의 이동 벡터 (전역 픽셀/초). */
data class Motion(
    val vxPxPerSec: Double,
    val vyPxPerSec: Double,
    /** 0..1, 에코가 충분하고 정합 오차가 작을수록 높다. */
    val confidence: Double,
) {
    /** 지상 속도(km/h) */
    fun speedKmh(lat: Double, zoom: Int): Double =
        hypot(vxPxPerSec, vyPxPerSec) * WebMercator.metersPerPixel(lat, zoom) * 3.6

    /** 이동 방향(진행 방향, 북=0°, 시계 방향) */
    fun headingDeg(): Double {
        val deg = Math.toDegrees(atan2(vxPxPerSec, -vyPxPerSec))
        return (deg + 360.0) % 360.0
    }

    companion object {
        val NONE = Motion(0.0, 0.0, 0.0)
    }
}

/**
 * 연속 레이더 프레임 사이의 전역 이동 벡터를 블록 정합(평균 절대 오차 최소화)으로 추정한다.
 * 거친 해상도에서 넓게 찾은 뒤 원 해상도에서 좁게 다듬는다.
 */
object MotionEstimator {
    /** 이 반사도 이상만 정합에 사용 */
    private const val ECHO_DBZ = 15f
    private const val MIN_ECHO_PIXELS = 150
    private const val COARSE_FACTOR = 4
    /** 거친 단계 탐색 반경(거친 픽셀). 4 × 8 = 32px ≈ 10분에 32km → 190km/h까지 */
    private const val COARSE_RADIUS = 8
    private const val FINE_RADIUS = COARSE_FACTOR

    fun estimate(frames: List<RadarFrame>): Motion {
        val sorted = frames.sortedBy { it.timeEpochSec }
        val pairs = sorted.zipWithNext().mapNotNull { (a, b) -> estimatePair(a, b) }
        if (pairs.isEmpty()) return Motion.NONE
        val weight = pairs.sumOf { it.confidence }
        if (weight <= 0.0) return Motion.NONE
        return Motion(
            vxPxPerSec = pairs.sumOf { it.vxPxPerSec * it.confidence } / weight,
            vyPxPerSec = pairs.sumOf { it.vyPxPerSec * it.confidence } / weight,
            confidence = weight / pairs.size,
        )
    }

    /** 두 프레임 사이 이동. 영역이 다르거나 에코가 부족하면 null. */
    fun estimatePair(prev: RadarFrame, cur: RadarFrame): Motion? {
        if (prev.originX != cur.originX || prev.originY != cur.originY ||
            prev.width != cur.width || prev.height != cur.height || prev.zoom != cur.zoom
        ) return null
        val dt = (cur.timeEpochSec - prev.timeEpochSec).toDouble()
        if (dt <= 0) return null

        val echo = cur.dbz.count { it >= ECHO_DBZ }
        if (echo < MIN_ECHO_PIXELS) return null

        val pc = downsample(prev, COARSE_FACTOR)
        val cc = downsample(cur, COARSE_FACTOR)
        val coarse = bestShift(pc, cc, 0, 0, COARSE_RADIUS) ?: return null
        val fine = bestShift(
            prev.toGrid(), cur.toGrid(),
            coarse.dx * COARSE_FACTOR, coarse.dy * COARSE_FACTOR, FINE_RADIUS,
        ) ?: return null

        // 오차가 정지 가정 대비 얼마나 줄었는지로 신뢰도 산정
        val still = bestShift(prev.toGrid(), cur.toGrid(), 0, 0, 0)?.cost ?: fine.cost
        val improvement = if (still > 0) (1.0 - fine.cost / still).coerceIn(0.0, 1.0) else 0.0
        val coverage = (echo / 2_000.0).coerceAtMost(1.0)
        return Motion(fine.dx / dt, fine.dy / dt, (0.3 + 0.7 * improvement) * coverage)
    }

    private class Grid(val w: Int, val h: Int, val v: FloatArray) {
        operator fun get(x: Int, y: Int) = if (x in 0 until w && y in 0 until h) v[y * w + x] else RadarFrame.NO_ECHO
    }

    private data class Shift(val dx: Int, val dy: Int, val cost: Double)

    private fun RadarFrame.toGrid() = Grid(width, height, dbz)

    private fun downsample(f: RadarFrame, k: Int): Grid {
        val w = f.width / k
        val h = f.height / k
        val out = FloatArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var m = RadarFrame.NO_ECHO
            for (j in 0 until k) for (i in 0 until k) m = maxOf(m, f[x * k + i, y * k + j])
            out[y * w + x] = m
        }
        return Grid(w, h, out)
    }

    /**
     * cur(x, y) ≈ prev(x - dx, y - dy) 를 만족하는 (dx, dy)를 [cx±r, cy±r]에서 찾는다.
     * 비용은 cur 또는 prev 위치에 에코가 있는 픽셀의 평균 절대 오차.
     */
    private fun bestShift(prev: Grid, cur: Grid, cx: Int, cy: Int, r: Int): Shift? {
        val echoIdx = ArrayList<Int>()
        for (i in cur.v.indices) if (cur.v[i] >= ECHO_DBZ) echoIdx += i
        if (echoIdx.isEmpty()) return null
        var best: Shift? = null
        for (dy in cy - r..cy + r) for (dx in cx - r..cx + r) {
            var sum = 0.0
            var n = 0
            for (i in echoIdx) {
                val x = i % cur.w
                val y = i / cur.w
                val sx = x - dx
                val sy = y - dy
                if (sx !in 0 until prev.w || sy !in 0 until prev.h) continue
                sum += abs(clip(cur.v[i]) - clip(prev[sx, sy]))
                n++
            }
            if (n < echoIdx.size / 2) continue // 겹치는 영역이 너무 작으면 제외
            val cost = sum / n
            // 동점이면 이동량이 작은 쪽을 선호
            if (best == null || cost < best.cost - 1e-9 ||
                (abs(cost - best.cost) <= 1e-9 && dx * dx + dy * dy < best.dx * best.dx + best.dy * best.dy)
            ) best = Shift(dx, dy, cost)
        }
        return best
    }

    private fun clip(dbz: Float) = if (dbz < ECHO_DBZ) 0f else dbz
}
