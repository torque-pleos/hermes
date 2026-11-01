package com.zigae.hermes.radar

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

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

/** 블록 단위 이동 벡터장 (격자 픽셀/초). 좌표는 최신 프레임 격자 기준. */
class MotionField(
    private val block: Int,
    private val cols: Int,
    private val rows: Int,
    private val vx: DoubleArray,
    private val vy: DoubleArray,
) {
    /** 격자 좌표의 벡터 (블록 중심 사이 쌍선형 보간) */
    fun at(gx: Double, gy: Double): Pair<Double, Double> {
        val fx = (gx / block - 0.5).coerceIn(0.0, (cols - 1).toDouble())
        val fy = (gy / block - 0.5).coerceIn(0.0, (rows - 1).toDouble())
        val x0 = fx.toInt(); val y0 = fy.toInt()
        val x1 = minOf(x0 + 1, cols - 1); val y1 = minOf(y0 + 1, rows - 1)
        val tx = fx - x0; val ty = fy - y0
        fun lerp(a: DoubleArray): Double {
            val top = a[y0 * cols + x0] * (1 - tx) + a[y0 * cols + x1] * tx
            val bottom = a[y1 * cols + x0] * (1 - tx) + a[y1 * cols + x1] * tx
            return top * (1 - ty) + bottom * ty
        }
        return lerp(vx) to lerp(vy)
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
    private const val FIELD_BLOCK = 128
    private const val FIELD_RADIUS = 6
    private const val MIN_BLOCK_ECHO = 300

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

    /**
     * 블록별 이동 벡터장. 블록마다 전역 벡터 주변(±[FIELD_RADIUS]px)에서 다시 정합하고,
     * 3×3 이웃 중 직접 추정한 블록끼리 평균하고, 주변에 추정값이 없는 블록은 전역 벡터를 쓴다.
     */
    fun estimateField(frames: List<RadarFrame>, global: Motion): MotionField? {
        val sorted = frames.sortedBy { it.timeEpochSec }
        val last = sorted.lastOrNull() ?: return null
        if (global.confidence <= 0.0) return null
        val cols = (last.width + FIELD_BLOCK - 1) / FIELD_BLOCK
        val rows = (last.height + FIELD_BLOCK - 1) / FIELD_BLOCK
        val sumX = DoubleArray(cols * rows)
        val sumY = DoubleArray(cols * rows)
        val weight = DoubleArray(cols * rows)
        for ((prev, cur) in sorted.zipWithNext()) {
            if (prev.width != cur.width || prev.height != cur.height || prev.originX != cur.originX ||
                prev.originY != cur.originY
            ) continue
            val dt = (cur.timeEpochSec - prev.timeEpochSec).toDouble()
            if (dt <= 0) continue
            val pg = prev.toGrid()
            val cg = cur.toGrid()
            val gx = (global.vxPxPerSec * dt).roundToInt()
            val gy = (global.vyPxPerSec * dt).roundToInt()
            for (by in 0 until rows) for (bx in 0 until cols) {
                val idx = ArrayList<Int>()
                for (y in by * FIELD_BLOCK until minOf((by + 1) * FIELD_BLOCK, cur.height))
                    for (x in bx * FIELD_BLOCK until minOf((bx + 1) * FIELD_BLOCK, cur.width)) {
                        val i = y * cur.width + x
                        if (cur.dbz[i] >= ECHO_DBZ) idx += i
                    }
                if (idx.size < MIN_BLOCK_ECHO) continue
                val shift = bestShift(pg, cg, gx, gy, FIELD_RADIUS, idx) ?: continue
                val b = by * cols + bx
                val w = idx.size.toDouble()
                sumX[b] += shift.dx / dt * w
                sumY[b] += shift.dy / dt * w
                weight[b] += w
            }
        }
        // 3×3 평활: 직접 추정한 이웃 블록만 가중 평균, 이웃에 추정값이 없으면 전역 벡터
        val sx = DoubleArray(cols * rows)
        val sy = DoubleArray(cols * rows)
        for (by in 0 until rows) for (bx in 0 until cols) {
            var ax = 0.0; var ay = 0.0; var wsum = 0.0
            for (j in -1..1) for (i in -1..1) {
                val x = bx + i; val y = by + j
                if (x !in 0 until cols || y !in 0 until rows) continue
                val b = y * cols + x
                if (weight[b] <= 0) continue
                // 자기 블록에 더 큰 비중
                val k = if (i == 0 && j == 0) 2.0 else 1.0
                ax += sumX[b] / weight[b] * k; ay += sumY[b] / weight[b] * k; wsum += k
            }
            sx[by * cols + bx] = if (wsum > 0) ax / wsum else global.vxPxPerSec
            sy[by * cols + bx] = if (wsum > 0) ay / wsum else global.vyPxPerSec
        }
        return MotionField(FIELD_BLOCK, cols, rows, sx, sy)
    }

    private class Grid(val w: Int, val h: Int, val v: FloatArray) {
        operator fun get(x: Int, y: Int) = if (x in 0 until w && y in 0 until h) v[y * w + x] else RadarFrame.NO_DATA
    }

    private data class Shift(val dx: Int, val dy: Int, val cost: Double)

    private fun RadarFrame.toGrid() = Grid(width, height, dbz)

    private fun downsample(f: RadarFrame, k: Int): Grid {
        val w = f.width / k
        val h = f.height / k
        val out = FloatArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var m = RadarFrame.NO_DATA
            for (j in 0 until k) for (i in 0 until k) {
                val v = f[x * k + i, y * k + j]
                if (!v.isNaN()) m = if (m.isNaN()) v else maxOf(m, v)
            }
            out[y * w + x] = m
        }
        return Grid(w, h, out)
    }

    /**
     * cur(x, y) ≈ prev(x - dx, y - dy) 를 만족하는 (dx, dy)를 [cx±r, cy±r]에서 찾는다.
     * 비용은 cur 또는 prev 위치에 에코가 있는 픽셀의 평균 절대 오차.
     */
    private fun bestShift(
        prev: Grid,
        cur: Grid,
        cx: Int,
        cy: Int,
        r: Int,
        echoIdx: List<Int> = cur.v.indices.filter { cur.v[it] >= ECHO_DBZ },
    ): Shift? {
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
                val pv = prev[sx, sy]
                if (pv.isNaN()) continue // 받지 않은 타일은 비교에서 제외
                sum += abs(clip(cur.v[i]) - clip(pv))
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
