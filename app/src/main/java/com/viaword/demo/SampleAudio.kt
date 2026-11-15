package com.viaword.demo

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.viaword.demo.data.AppCatalog

internal class SampleAudio(private val context: Context) {
    private var player: MediaPlayer? = null
    var playingWord by mutableStateOf<String?>(null)
        private set
    var failedWord by mutableStateOf<String?>(null)
        private set

    fun play(wordId: String, onResult: (Boolean) -> Unit) {
        stop()
        val next = try {
            AppCatalog.audio[wordId]?.let { MediaPlayer.create(context, it) }
        } catch (_: RuntimeException) {
            null
        }
        if (next == null) {
            failedWord = wordId
            onResult(false)
            return
        }
        player = next
        playingWord = wordId
        fun finish(success: Boolean) {
            if (player !== next) return
            stop()
            if (!success) failedWord = wordId
            onResult(success)
        }
        next.setOnCompletionListener { finish(true) }
        next.setOnErrorListener { _, _, _ -> finish(false); true }
        try {
            next.start()
        } catch (_: IllegalStateException) {
            finish(false)
        }
    }

    fun stop() {
        val old = player
        player = null
        old?.release()
        playingWord = null
        failedWord = null
    }
}
