package com.zigae.hermes.ui

import com.zigae.hermes.radar.PointForecast

/** "경유 1 대전 물류센터" → "대전 물류센터", "도착 부산신항" → "부산신항" */
internal fun short(label: String): String {
    val parts = label.split(' ')
    return when (parts.first()) {
        "경유" -> parts.drop(2).joinToString(" ")
        "도착" -> parts.drop(1).joinToString(" ")
        else -> label
    }
}

/** 정차지 종류 라벨: 출발 / 경유 1 / 도착 */
internal fun kind(label: String, index: Int): String = when {
    index == 0 -> "출발"
    label.startsWith("경유") -> label.split(' ').take(2).joinToString(" ")
    label.startsWith("도착") -> "도착"
    else -> ""
}

internal fun relative(sec: Long): String {
    val m = (sec / 60).coerceAtLeast(0)
    return if (m >= 60) "${m / 60}시간 ${m % 60}분 후" else "${m}분 후"
}

private val compassKo = listOf("북", "북동", "동", "남동", "남", "남서", "서", "북서")
internal fun compassKo(deg: Double) = compassKo[((deg + 22.5) / 45).toInt() % 8]

/** 도착 시 강수 짧은 문구 */
internal fun arrivalText(f: PointForecast): String = when (f) {
    is PointForecast.BeyondHorizon -> "예보 범위 밖"
    PointForecast.OutOfCoverage -> "정보 없음"
    else -> f.rainLevel?.label ?: "정보 없음"
}

/** 근거(출처) 한 줄 */
internal fun sourceText(f: PointForecast): String = when (f) {
    is PointForecast.Fleet -> "주변 차량 ${f.evidence.vehicles}대(비 ${f.evidence.wetVehicles}) · " + sourceText(f.base)
    is PointForecast.Rain -> "레이더 ${f.leadMinutes}분 예측 · %.1fmm/h".format(f.rateMmPerHour)
    is PointForecast.Kma -> "기상청 예보" + (f.pop?.let { " · 강수확률 $it%" } ?: "") + if (f.rateMmPerHour > 0) " · %.1fmm/h".format(f.rateMmPerHour) else ""
    is PointForecast.BeyondHorizon -> "레이더 예측 범위 밖 · 지금 그곳 ${f.observedNow?.label ?: "정보 없음"}"
    PointForecast.OutOfCoverage -> "레이더·예보 범위 밖"
}
