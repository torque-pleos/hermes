package com.zigae.hermes.weather

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/** 기상청 동네예보 격자 (Lambert Conformal Conic, 5km). 기상청 공개 변환식. */
data class KmaCell(val nx: Int, val ny: Int)

object KmaGrid {
    private const val RE = 6371.00877 // 지구 반경(km)
    private const val GRID = 5.0 // 격자 간격(km)
    private const val SLAT1 = 30.0
    private const val SLAT2 = 60.0
    private const val OLON = 126.0
    private const val OLAT = 38.0
    private const val XO = 43.0
    private const val YO = 136.0

    private val degrad = PI / 180.0
    private val re = RE / GRID
    private val slat1 = SLAT1 * degrad
    private val slat2 = SLAT2 * degrad
    private val olon = OLON * degrad
    private val olat = OLAT * degrad
    private val sn = ln(cos(slat1) / cos(slat2)) / ln(tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5))
    private val sf = tan(PI * 0.25 + slat1 * 0.5).pow(sn) * cos(slat1) / sn
    private val ro = re * sf / tan(PI * 0.25 + olat * 0.5).pow(sn)

    fun toCell(lat: Double, lon: Double): KmaCell {
        val ra = re * sf / tan(PI * 0.25 + lat * degrad * 0.5).pow(sn)
        var theta = lon * degrad - olon
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn
        return KmaCell(floor(ra * sin(theta) + XO + 0.5).toInt(), floor(ro - ra * cos(theta) + YO + 0.5).toInt())
    }
}
