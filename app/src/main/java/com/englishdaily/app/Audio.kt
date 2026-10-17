package com.englishdaily.app

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.Locale

class Speaker(private val ctx: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var counter = 0
    var rate = 0.9f

    var speaking by mutableStateOf(false)
        private set

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.US)
                ready = r != null && r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { speaking = true }
                    override fun onDone(utteranceId: String?) { speaking = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { speaking = false }
                })
            }
        }
    }

    fun speak(text: String) {
        if (!ready) {
            Toast.makeText(
                ctx,
                "صوت اللغة الإنجليزية غير جاهز. ثبّت بيانات الصوت من إعدادات تحويل النص إلى كلام في الهاتف.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        tts?.setSpeechRate(rate)
        counter++
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u$counter")
    }

    fun stop() {
        tts?.stop()
        speaking = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

class Recorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    var file: File? = null
        private set

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()

    /** Starts recording to a file named after the topic. */
    fun start(topic: String): Boolean {
        val dir = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "Recordings")
        dir.mkdirs()
        val safe = topic.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)
        var f = File(dir, "$safe.m4a")
        var k = 2
        while (f.exists()) {
            f = File(dir, "$safe ($k).m4a")
            k++
        }
        val r = newRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            rec = r
            file = f
            true
        } catch (e: Exception) {
            try { r.release() } catch (_: Exception) {}
            false
        }
    }

    fun stop(): Boolean {
        val r = rec ?: return false
        rec = null
        return try {
            r.stop()
            true
        } catch (e: Exception) {
            file?.delete()
            false
        } finally {
            r.release()
        }
    }

    fun cancel() {
        val r = rec ?: return
        rec = null
        try { r.stop() } catch (_: Exception) {}
        r.release()
        file?.delete()
    }
}

class Player {
    private var mp: MediaPlayer? = null

    var playing by mutableStateOf<String?>(null)
        private set

    fun toggle(path: String) {
        if (playing == path) {
            stop()
            return
        }
        stop()
        try {
            val m = MediaPlayer()
            m.setDataSource(path)
            m.prepare()
            m.setOnCompletionListener { stop() }
            m.start()
            mp = m
            playing = path
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        try { mp?.stop() } catch (_: Exception) {}
        mp?.release()
        mp = null
        playing = null
    }
}
