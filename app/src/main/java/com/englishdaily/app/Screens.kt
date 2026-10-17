package com.englishdaily.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- Words

@Composable
fun WordsScreen(app: AppModel, back: () -> Unit) {
    val n = app.store.lessonNo()
    val words = app.lessons[n % app.lessons.size].words
    var i by remember { mutableStateOf(0) }
    var done by remember { mutableStateOf(app.store.wordsDone(n)) }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("كلمات اليوم (${done.size}/${words.size})", back)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            words.indices.forEach { k ->
                FilterChip(
                    selected = k == i,
                    onClick = { i = k },
                    label = { Text("${k + 1}${if (k in done) " ✓" else ""}") }
                )
            }
        }
        key(i) {
            val w = words[i]
            var hidden by remember { mutableStateOf(false) }
            var typed by remember { mutableStateOf("") }
            var sentence by remember { mutableStateOf(app.store.sentence(n, i)) }
            val spelled = typed.trim().equals(w.en, ignoreCase = true)
            val sentenceOk = sentence.trim().split(Regex("\\s+")).count { it.isNotEmpty() } >= 3

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        En(
                            if (hidden) "• ".repeat(w.en.length).trim() else w.en,
                            style = MaterialTheme.typography.displaySmall, center = true
                        )
                        Text(w.ar, style = MaterialTheme.typography.headlineSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { app.speaker.speak(w.en) }) { Text("🔊 انطق") }
                            OutlinedButton(onClick = { hidden = !hidden }) {
                                Text(if (hidden) "أظهر الكلمة" else "أخفِ الكلمة")
                            }
                        }
                        En(w.ex, style = MaterialTheme.typography.bodyMedium, center = true)
                        TextButton(onClick = { app.speaker.speak(w.ex) }) { Text("🔊 اسمع المثال") }
                    }
                }
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    label = { Text(if (spelled) "اكتب الكلمة ✅" else "اكتب الكلمة بالإنجليزية") }
                )
                OutlinedTextField(
                    value = sentence,
                    onValueChange = { sentence = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    label = { Text("اكتب جملة تستخدم فيها الكلمة (3 كلمات على الأقل)") }
                )
                OutlinedButton(
                    onClick = { app.speaker.speak(sentence) },
                    enabled = sentence.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🔊 اقرأ جملتي بصوت عالٍ") }
                Button(
                    enabled = spelled && sentenceOk,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val first = "words" !in app.store.steps(n)
                        app.store.markWord(n, i, sentence.trim())
                        done = done + i
                        if (first && done.size >= words.size) {
                            app.store.markStep(n, "words")
                            app.bump()
                            back()
                        } else {
                            app.bump()
                            i = (i + 1) % words.size
                        }
                    }
                ) { Text(if (i in done) "تحديث وحفظ ✔" else "حفظ الكلمة ✔") }
            }
        }
    }
}

// ---------------------------------------------------------------- Story

@Composable
fun StoryScreen(app: AppModel, back: () -> Unit) {
    val n = app.store.lessonNo()
    val story = app.lessons[n % app.lessons.size].story
    var rate by remember { mutableStateOf(app.store.ttsRate) }
    DisposableEffect(Unit) { onDispose { app.speaker.stop() } }

    Column(Modifier.fillMaxSize()) {
        TopBar("قصة اليوم", back)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            En(story.title, style = MaterialTheme.typography.headlineMedium)
            Card(Modifier.fillMaxWidth()) {
                En(
                    story.text,
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
                )
            }
            Text("سرعة القراءة")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("بطيئة" to 0.7f, "عادية" to 0.9f, "سريعة" to 1.1f).forEach { (label, r) ->
                    FilterChip(selected = rate == r, onClick = {
                        rate = r
                        app.store.ttsRate = r
                        app.speaker.rate = r
                    }, label = { Text(label) })
                }
            }
            Button(
                onClick = {
                    if (app.speaker.speaking) app.speaker.stop() else app.speaker.speak(story.text)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (app.speaker.speaking) "⏹ إيقاف" else "▶ استمع للقصة") }
            OutlinedButton(
                onClick = {
                    app.store.markStep(n, "story")
                    app.bump()
                    back()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("أنهيت الاستماع ✔") }
        }
    }
}

// ---------------------------------------------------------------- Grammar

@Composable
fun GrammarScreen(app: AppModel, back: () -> Unit) {
    val n = app.store.lessonNo()
    val g = app.lessons[n % app.lessons.size].grammar
    Column(Modifier.fillMaxSize()) {
        TopBar("قاعدة اليوم", back)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(g.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth()) {
                Text(g.explain, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
            Text("أمثلة", fontWeight = FontWeight.Bold)
            g.examples.forEach { ex ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        En(ex, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { app.speaker.speak(ex) }) { Text("🔊") }
                    }
                }
            }
            Button(
                onClick = {
                    app.store.markStep(n, "grammar")
                    app.bump()
                    back()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("فهمت القاعدة ✔") }
        }
    }
}

// ---------------------------------------------------------------- Speaking

private val topicIdeas = listOf(
    "My daily routine", "My family", "My favorite food", "My city",
    "My hobbies", "A trip I remember", "My dream job", "My best friend"
)

@Composable
fun SpeakScreen(app: AppModel, back: () -> Unit) {
    val ctx = LocalContext.current
    val n = app.store.lessonNo()
    val minSecs = 50
    var topic by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0) }
    var recs by remember { mutableStateOf(app.store.recordings()) }
    val rec = remember { Recorder(ctx) }

    DisposableEffect(Unit) {
        onDispose {
            rec.cancel()
            app.player.stop()
        }
    }
    LaunchedEffect(recording) {
        if (recording) {
            val t0 = SystemClock.elapsedRealtime()
            while (true) {
                elapsed = ((SystemClock.elapsedRealtime() - t0) / 1000).toInt()
                delay(250)
            }
        }
    }

    fun startRec() {
        if (rec.start(topic)) {
            elapsed = 0
            recording = true
        } else {
            Toast.makeText(ctx, "تعذّر بدء التسجيل", Toast.LENGTH_SHORT).show()
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startRec()
        else Toast.makeText(ctx, "نحتاج إذن الميكروفون لتسجيل صوتك", Toast.LENGTH_LONG).show()
    }

    fun finishRec() {
        val secs = elapsed
        val f = rec.file
        if (rec.stop() && f != null) {
            app.store.addRecording(Recording(topic.trim(), f.absolutePath, secs, app.store.today()))
            recs = app.store.recordings()
            app.store.markStep(n, "speak")
            app.bump()
            Toast.makeText(ctx, "تم حفظ التسجيل ✔", Toast.LENGTH_SHORT).show()
        }
        recording = false
        topic = ""
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("تحدّث بالإنجليزية", back)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!recording) {
                Text("اختر موضوعًا وتحدث عنه بالإنجليزية لمدة $minSecs ثانية على الأقل. سيُسمّى التسجيل باسم الموضوع.")
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    label = { Text("موضوع الحديث") }
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    topicIdeas.forEach { t ->
                        AssistChip(onClick = { topic = t }, label = { En(t, style = MaterialTheme.typography.bodySmall) })
                    }
                }
                Button(
                    enabled = topic.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED
                        ) startRec() else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) { Text("🎙 ابدأ التسجيل") }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        En(topic.trim(), style = MaterialTheme.typography.titleLarge, center = true)
                        En("%02d:%02d".format(elapsed / 60, elapsed % 60), style = MaterialTheme.typography.displayMedium)
                        LinearProgressIndicator(
                            progress = minOf(1f, elapsed / minSecs.toFloat()),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            if (elapsed >= minSecs) "ممتاز! يمكنك الإنهاء الآن."
                            else "تحدث... باقي ${minSecs - elapsed} ثانية على الحد الأدنى"
                        )
                    }
                }
                Button(
                    onClick = { finishRec() },
                    enabled = elapsed >= minSecs,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⏹ إنهاء وحفظ") }
                OutlinedButton(
                    onClick = {
                        rec.cancel()
                        recording = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("إلغاء التسجيل") }
            }

            if (recs.isNotEmpty()) {
                Text("تسجيلاتك", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                recs.forEach { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                En(r.topic, style = MaterialTheme.typography.titleMedium)
                                Text("${r.secs} ثانية  •  ${r.date}", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { app.player.toggle(r.path) }) {
                                Text(if (app.player.playing == r.path) "⏹" else "▶")
                            }
                            TextButton(onClick = {
                                if (app.player.playing == r.path) app.player.stop()
                                app.store.deleteRecording(r)
                                recs = app.store.recordings()
                            }) { Text("🗑") }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Quiz

data class Q(
    val prompt: String,
    val speak: String?,
    val show: String?,
    val opts: List<String>,
    val answer: Int,
    val ltr: Boolean
)

private fun mk(prompt: String, speak: String?, show: String?, correct: String, wrong: List<String>, ltr: Boolean): Q {
    val opts = (listOf(correct) + wrong.filter { it != correct }.distinct().shuffled().take(3)).shuffled()
    return Q(prompt, speak, show, opts, opts.indexOf(correct), ltr)
}

fun buildQuiz(app: AppModel, n: Int): List<Q> {
    val lesson = app.lessons[n % app.lessons.size]
    val words = lesson.words.shuffled()
    val pool = app.allWords
    val qs = mutableListOf<Q>()
    words.take(5).forEach { w ->
        qs += mk("ما معنى هذه الكلمة؟", null, w.en, w.ar, pool.filter { it.en != w.en }.map { it.ar }, false)
    }
    words.drop(5).forEach { w ->
        qs += mk("استمع ثم اختر الكلمة التي سمعتها", w.en, null, w.en, pool.filter { it.en != w.en }.map { it.en }, true)
    }
    val s = lesson.story
    qs += mk("سؤال عن قصة اليوم", null, s.q, s.opts[s.a], s.opts, true)
    val g = lesson.grammar
    qs += mk("سؤال عن قاعدة اليوم", null, g.q, g.opts[g.a], g.opts, true)
    return qs
}

@Composable
fun QuizScreen(app: AppModel, back: () -> Unit) {
    val n = app.store.lessonNo()
    val qs = remember { buildQuiz(app, n) }
    var idx by remember { mutableStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableStateOf(0) }
    val wrong = remember { mutableStateListOf<Q>() }
    DisposableEffect(Unit) { onDispose { app.speaker.stop() } }

    Column(Modifier.fillMaxSize()) {
        TopBar("اختبار اليوم", back)
        if (idx >= qs.size) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val pct = score * 100 / qs.size
                Text(
                    (if (pct >= 80) "🎉 ممتاز! " else if (pct >= 50) "👍 جيد! " else "💪 تابع التدرّب! ") + "نتيجتك $score من ${qs.size}",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                )
                if (wrong.isNotEmpty()) {
                    Text("راجع أخطاءك:", fontWeight = FontWeight.Bold)
                    wrong.forEach { q ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                En((q.show ?: q.speak ?: ""), style = MaterialTheme.typography.bodyMedium)
                                if (q.ltr) En("✔ " + q.opts[q.answer], style = MaterialTheme.typography.titleMedium)
                                else Text("✔ " + q.opts[q.answer], style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        app.store.addQuiz(n, score, qs.size)
                        app.store.markStep(n, "quiz")
                        app.bump()
                        back()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("إنهاء الدرس ✔") }
            }
        } else {
            val q = qs[idx]
            LaunchedEffect(idx) { q.speak?.let { app.speaker.speak(it) } }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(progress = idx / qs.size.toFloat(), modifier = Modifier.fillMaxWidth())
                Text("السؤال ${idx + 1} من ${qs.size}", style = MaterialTheme.typography.bodySmall)
                Text(q.prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (q.show != null) En(q.show, style = MaterialTheme.typography.headlineMedium)
                if (q.speak != null) {
                    OutlinedButton(onClick = { app.speaker.speak(q.speak) }) { Text("🔊 أعد الاستماع") }
                }
                q.opts.forEachIndexed { k, o ->
                    val bg = when {
                        picked == null -> MaterialTheme.colorScheme.surfaceVariant
                        k == q.answer -> Color(0xFF2E7D32).copy(alpha = 0.35f)
                        k == picked -> Color(0xFFC62828).copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Card(
                        Modifier.fillMaxWidth().clickable(enabled = picked == null) {
                            picked = k
                            if (k == q.answer) score++ else wrong += q
                        },
                        colors = CardDefaults.cardColors(containerColor = bg)
                    ) {
                        Box(Modifier.padding(16.dp)) {
                            if (q.ltr) En(o, style = MaterialTheme.typography.titleMedium)
                            else Text(o, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                if (picked != null) {
                    Button(
                        onClick = {
                            idx++
                            picked = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (idx == qs.size - 1) "عرض النتيجة" else "التالي") }
                }
            }
        }
    }
}
