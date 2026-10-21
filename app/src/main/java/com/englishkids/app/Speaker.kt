package com.englishkids.app

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speaker(ctx: Context) {
    private var ready = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.85f)
                ready = true
            }
        }
    }

    fun say(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mimo")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
