package com.zigae.hermes.vehicle

import ai.pleos.playground.vehicle.constant.control.WiperSwitch
import com.zigae.hermes.radar.RainLevel

/** 와이퍼 단계로 추정한 체감 강수 강도 (F4 레이더 × 와이퍼 교차 검증의 입력). */
enum class WiperRain(val label: String) {
    NONE("비 없음"),
    MIST("이슬비·물보라"),
    LIGHT("약한 비"),
    MODERATE("보통 비"),
    HEAVY("강한 비"),
    UNKNOWN("알 수 없음"),
}

fun WiperSwitch?.toRain(): WiperRain = when (this) {
    WiperSwitch.OFF -> WiperRain.NONE
    WiperSwitch.MIST -> WiperRain.MIST
    WiperSwitch.INTERMITTENT_LEVEL_1,
    WiperSwitch.INTERMITTENT_LEVEL_2,
    WiperSwitch.INTERMITTENT_LEVEL_3 -> WiperRain.LIGHT
    WiperSwitch.INTERMITTENT_LEVEL_4,
    WiperSwitch.INTERMITTENT_LEVEL_5,
    WiperSwitch.CONTINUOUS_LEVEL_1,
    WiperSwitch.CONTINUOUS_LEVEL_2 -> WiperRain.MODERATE
    WiperSwitch.CONTINUOUS_LEVEL_3,
    WiperSwitch.CONTINUOUS_LEVEL_4,
    WiperSwitch.CONTINUOUS_LEVEL_5 -> WiperRain.HEAVY
    // AUTO는 우적 센서 판단이라 단계만으로 강도를 알 수 없다.
    WiperSwitch.AUTO, WiperSwitch.SERVICE, WiperSwitch.OTHER, null -> WiperRain.UNKNOWN
}

/** 크라우드 관측용 강도. 알 수 없으면 null(올리지 않음) */
fun WiperRain.toRainLevel(): RainLevel? = when (this) {
    WiperRain.NONE -> RainLevel.NONE
    WiperRain.MIST, WiperRain.LIGHT -> RainLevel.LIGHT
    WiperRain.MODERATE -> RainLevel.MODERATE
    WiperRain.HEAVY -> RainLevel.HEAVY
    WiperRain.UNKNOWN -> null
}
