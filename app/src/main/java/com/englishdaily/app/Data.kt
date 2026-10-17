package com.englishdaily.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Word(val en: String, val ar: String, val ex: String)
data class Story(val title: String, val text: String, val q: String, val opts: List<String>, val a: Int)
data class Grammar(
    val title: String, val explain: String, val examples: List<String>,
    val q: String, val opts: List<String>, val a: Int
)
data class Lesson(val words: List<Word>, val story: Story, val grammar: Grammar)
data class Recording(val topic: String, val path: String, val secs: Int, val date: String)

private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

object Content {
    fun load(ctx: Context): List<Lesson> {
        val txt = ctx.assets.open("lessons.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(txt).getJSONArray("lessons")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ws = o.getJSONArray("words")
            val s = o.getJSONObject("story")
            val g = o.getJSONObject("grammar")
            Lesson(
                (0 until ws.length()).map { j ->
                    val w = ws.getJSONObject(j)
                    Word(w.getString("en"), w.getString("ar"), w.getString("ex"))
                },
                Story(
                    s.getString("title"), s.getString("text"), s.getString("q"),
                    s.getJSONArray("opts").strings(), s.getInt("a")
                ),
                Grammar(
                    g.getString("title"), g.getString("explain"),
                    g.getJSONArray("examples").strings(), g.getString("q"),
                    g.getJSONArray("opts").strings(), g.getInt("a")
                )
            )
        }
    }
}

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("english_daily", Context.MODE_PRIVATE)

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    var completedDays: Int
        get() = p.getInt("completed_days", 0)
        set(v) { p.edit().putInt("completed_days", v).apply() }

    private fun getSet(key: String): Set<String> =
        (p.getStringSet(key, emptySet()) ?: emptySet()).toSet()

    private fun putSet(key: String, s: Set<String>) {
        p.edit().putStringSet(key, s).apply()
    }

    val doneDates: Set<String> get() = getSet("done_dates")

    /** Absolute lesson number pinned for today (stays the same all day). */
    fun lessonNo(): Int {
        val key = "pin_" + today()
        if (!p.contains(key)) p.edit().putInt(key, completedDays).apply()
        return p.getInt(key, 0)
    }

    fun steps(n: Int): Set<String> = getSet("steps_$n")

    fun markStep(n: Int, step: String) {
        putSet("steps_$n", steps(n) + step)
        if (steps(n).size >= 5) {
            putSet("done_dates", doneDates + today())
            if (completedDays < n + 1) completedDays = n + 1
        }
    }

    fun wordsDone(n: Int): Set<Int> = getSet("wd_$n").map { it.toInt() }.toSet()

    fun markWord(n: Int, i: Int, sentence: String) {
        putSet("wd_$n", getSet("wd_$n") + i.toString())
        p.edit().putString("s_${n}_$i", sentence).apply()
    }

    fun sentence(n: Int, i: Int): String = p.getString("s_${n}_$i", "") ?: ""

    fun addQuiz(n: Int, score: Int, total: Int) {
        val old = p.getString("quiz_hist", "") ?: ""
        p.edit().putString("quiz_hist", old + "${today()}|${n + 1}|$score|$total;").apply()
    }

    fun quizHistory(): List<List<String>> =
        (p.getString("quiz_hist", "") ?: "").split(";")
            .filter { it.isNotBlank() }
            .map { it.split("|") }
            .filter { it.size == 4 }

    fun recordings(): List<Recording> {
        val a = JSONArray(p.getString("recs", "[]"))
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Recording(o.getString("topic"), o.getString("path"), o.getInt("secs"), o.getString("date"))
        }.filter { File(it.path).exists() }
    }

    private fun saveRecordings(list: List<Recording>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("topic", it.topic).put("path", it.path).put("secs", it.secs).put("date", it.date))
        }
        p.edit().putString("recs", a.toString()).apply()
    }

    fun addRecording(r: Recording) = saveRecordings(listOf(r) + recordings())

    fun deleteRecording(r: Recording) {
        File(r.path).delete()
        saveRecordings(recordings().filter { it.path != r.path })
    }

    fun streak(): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val done = doneDates
        if (!done.contains(fmt.format(cal.time))) cal.add(Calendar.DAY_OF_YEAR, -1)
        var n = 0
        while (done.contains(fmt.format(cal.time))) {
            n++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return n
    }

    var reminderOn: Boolean
        get() = p.getBoolean("rem_on", false)
        set(v) { p.edit().putBoolean("rem_on", v).apply() }
    var reminderH: Int
        get() = p.getInt("rem_h", 20)
        set(v) { p.edit().putInt("rem_h", v).apply() }
    var reminderM: Int
        get() = p.getInt("rem_m", 0)
        set(v) { p.edit().putInt("rem_m", v).apply() }
    var ttsRate: Float
        get() = p.getFloat("tts_rate", 0.9f)
        set(v) { p.edit().putFloat("tts_rate", v).apply() }
}
