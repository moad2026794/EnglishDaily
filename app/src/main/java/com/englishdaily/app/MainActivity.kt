package com.englishdaily.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max

class AppModel(ctx: Context) {
    val store = Store(ctx)
    val lessons: List<Lesson> = Content.load(ctx)
    val allWords: List<Word> = lessons.flatMap { it.words }
    val speaker = Speaker(ctx).also { it.rate = store.ttsRate }
    val player = Player()

    var tick by mutableIntStateOf(0)
        private set

    fun bump() { tick++ }

    /** All words the user has learned so far, with the sentence they wrote. */
    fun learned(): List<Pair<Word, String>> {
        val out = LinkedHashMap<String, Pair<Word, String>>()
        val upto = max(store.completedDays, store.lessonNo() + 1)
        for (k in 0 until upto) {
            val ws = lessons[k % lessons.size].words
            val done: Set<Int> = if (k < store.completedDays) ws.indices.toSet() else store.wordsDone(k)
            for (i in done.sorted()) {
                if (i in ws.indices) out.putIfAbsent(ws[i].en, ws[i] to store.sentence(k, i))
            }
        }
        return out.values.toList()
    }

    fun release() {
        speaker.shutdown()
        player.stop()
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var app: AppModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = AppModel(applicationContext)
        setContent { AppRoot(app) }
    }

    override fun onDestroy() {
        app.release()
        super.onDestroy()
    }
}
