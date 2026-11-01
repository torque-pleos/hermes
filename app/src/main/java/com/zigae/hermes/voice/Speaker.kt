package com.zigae.hermes.voice

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Android 표준 TextToSpeech 래퍼 (Pleos TTS SDK는 Playground에서 사용 불가).
 *
 * Pleos Connect는 시스템 기본 TTS 엔진(tts_default_synth)이 비어 있어 기본 생성자로는 초기화가 실패한다.
 * 기본 엔진 실패 시 TTS_SERVICE를 제공하는 설치 엔진을 찾아 명시적으로 지정한다.
 */
class Speaker(context: Context) {

    private val appContext = context.applicationContext
    private val _status = MutableStateFlow("초기화 중")
    val status: StateFlow<String> = _status.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private val candidates: ArrayDeque<String?> = ArrayDeque<String?>().apply {
        add(null) // 시스템 기본 엔진
        addAll(installedEngines())
    }

    init {
        connectNext()
    }

    private fun installedEngines(): List<String> =
        appContext.packageManager
            .queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
            .map { it.serviceInfo.packageName }
            .distinct()

    private fun connectNext() {
        if (candidates.isEmpty()) {
            _status.value = "사용 가능한 TTS 엔진 없음"
            return
        }
        val engine = candidates.removeFirst()
        Log.i(TAG, "connect engine=${engine ?: "(default)"}")
        lateinit var created: TextToSpeech
        // 기본 엔진이 없으면 생성자 안에서 동기적으로 실패 콜백이 오므로, 할당 이후로 미뤄 처리한다.
        val listener = TextToSpeech.OnInitListener { code -> mainHandler.post { onInit(created, engine, code) } }
        created = if (engine == null) TextToSpeech(appContext, listener) else TextToSpeech(appContext, listener, engine)
        tts = created
    }

    private fun onInit(engineTts: TextToSpeech, engine: String?, code: Int) {
        if (code != TextToSpeech.SUCCESS) {
            Log.w(TAG, "init failed engine=${engine ?: "(default)"} code=$code")
            engineTts.shutdown()
            connectNext()
            return
        }
        val lang = engineTts.setLanguage(Locale.KOREAN)
        ready = lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED
        _status.value = "엔진=${engine ?: engineTts.defaultEngine} · 한국어=${if (ready) "지원" else "미지원($lang)"}"
        Log.i(TAG, _status.value)
        engineTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = log("재생 시작")
            override fun onDone(utteranceId: String?) = log("재생 완료")

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = log("재생 오류")
            override fun onError(utteranceId: String?, errorCode: Int) = log("재생 오류 ($errorCode)")
        })
    }

    fun speak(text: String) {
        val engineTts = tts
        if (!ready || engineTts == null) {
            log("준비 안 됨")
            return
        }
        val result = engineTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hermes-${System.nanoTime()}")
        log(if (result == TextToSpeech.SUCCESS) "speak 요청됨" else "speak 실패 ($result)")
    }

    fun shutdown() {
        tts?.shutdown()
    }

    private fun log(msg: String) {
        Log.i(TAG, msg)
        _status.value = "${_status.value.substringBefore(" | ")} | $msg"
    }

    private companion object {
        const val TAG = "Hermes/TTS"
    }
}
