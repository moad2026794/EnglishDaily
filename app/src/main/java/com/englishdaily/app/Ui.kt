package com.englishdaily.app

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** English text is always drawn left-to-right inside the RTL app. */
@Composable
fun En(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    center: Boolean = false
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text,
            modifier = modifier,
            style = if (center) style.copy(textAlign = TextAlign.Center) else style
        )
    }
}

@Composable
fun TopBar(title: String, back: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = back) { Text("→ رجوع") }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppRoot(app: AppModel) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF6FCFB8))
    } else {
        lightColorScheme(primary = Color(0xFF2E7D6B))
    }
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                var route by remember { mutableStateOf("home") }
                var tab by remember { mutableStateOf(0) }
                val home = {
                    app.speaker.stop()
                    app.player.stop()
                    app.bump()
                    route = "home"
                }
                BackHandler(enabled = route != "home") { home() }
                when (route) {
                    "words" -> WordsScreen(app, home)
                    "story" -> StoryScreen(app, home)
                    "grammar" -> GrammarScreen(app, home)
                    "speak" -> SpeakScreen(app, home)
                    "quiz" -> QuizScreen(app, home)
                    else -> HomeScaffold(app, tab, { tab = it }) { route = it }
                }
            }
        }
    }
}

@Composable
fun HomeScaffold(app: AppModel, tab: Int, setTab: (Int) -> Unit, go: (String) -> Unit) {
    Scaffold(bottomBar = {
        NavigationBar {
            listOf("اليوم" to "📅", "المراجعة" to "📚", "التقدم" to "📊").forEachIndexed { i, item ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { setTab(i) },
                    icon = { Text(item.second) },
                    label = { Text(item.first) }
                )
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> TodayTab(app, go)
                1 -> ReviewTab(app)
                else -> ProgressTab(app)
            }
        }
    }
}

@Composable
fun StepCard(icon: String, title: String, sub: String, done: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier.padding(16.dp).alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(sub, style = MaterialTheme.typography.bodySmall)
            }
            if (done) Text("✅", fontSize = 24.sp)
        }
    }
}

@Composable
fun TodayTab(app: AppModel, go: (String) -> Unit) {
    app.tick
    val n = app.store.lessonNo()
    val steps = app.store.steps(n)
    val wordCount = app.lessons[n % app.lessons.size].words.size
    val doneWords = app.store.wordsDone(n).size
    val streak = app.store.streak()
    val first4 = listOf("words", "story", "grammar", "speak").all { it in steps }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الدرس رقم ${n + 1}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("🔥 $streak يوم متتالٍ")
                LinearProgressIndicator(progress = steps.size / 5f, modifier = Modifier.fillMaxWidth())
                Text("أنجزت ${steps.size} من 5 خطوات", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (steps.size >= 5) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "🎉 أنهيت درس اليوم! أحسنت. نراك غدًا بدرس جديد.",
                    Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        StepCard("📝", "كلمات اليوم", "أنجزت $doneWords من $wordCount كلمات", "words" in steps, true) { go("words") }
        StepCard("📖", "قصة اليوم", "استمع إلى قصة قصيرة بصوت واضح", "story" in steps, true) { go("story") }
        StepCard("🧩", "قاعدة اليوم", "شرح قاعدة مع أمثلة", "grammar" in steps, true) { go("grammar") }
        StepCard("🎙", "تحدّث 50 ثانية", "سجّل صوتك عن موضوع تختاره", "speak" in steps, true) { go("speak") }
        StepCard(
            "✅", "اختبار اليوم",
            if (first4) "جاهز، اختبر ما تعلمته" else "يُفتح بعد إكمال الخطوات الأربع",
            "quiz" in steps, first4
        ) { go("quiz") }
    }
}

@Composable
fun ReviewTab(app: AppModel) {
    app.tick
    val items = app.learned()
    var cur by remember { mutableStateOf(0) }
    var flipped by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("مراجعة الكلمات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) {
            Text("ستظهر هنا الكلمات التي تتعلمها لتراجعها في أي وقت.")
        } else {
            val w = items[cur % items.size].first
            Card(Modifier.fillMaxWidth().clickable { flipped = !flipped }) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (flipped) Text(w.ar, style = MaterialTheme.typography.headlineMedium)
                    else En(w.en, style = MaterialTheme.typography.headlineMedium, center = true)
                    Text("اضغط على البطاقة لقلبها", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { app.speaker.speak(w.en) }) { Text("🔊 انطق") }
                OutlinedButton(onClick = {
                    cur = (items.indices).random()
                    flipped = false
                }) { Text("بطاقة عشوائية") }
            }
            Text("كل كلمات تعلمتها (${items.size})", fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { (word, sentence) ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    En(word.en, style = MaterialTheme.typography.titleMedium)
                                    Text("— ${word.ar}")
                                }
                                if (sentence.isNotBlank()) En(sentence, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { app.speaker.speak(word.en) }) { Text("🔊") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ProgressTab(app: AppModel) {
    app.tick
    val ctx = LocalContext.current
    val st = app.store
    val hist = st.quizHistory()
    val avg = if (hist.isEmpty()) 0 else hist.map { it[2].toInt() * 100 / maxOf(1, it[3].toInt()) }.average().toInt()
    var on by remember { mutableStateOf(st.reminderOn) }
    var h by remember { mutableStateOf(st.reminderH) }
    var m by remember { mutableStateOf(st.reminderM) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("تقدمك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("🔥 أيام متتالية", "${st.streak()}", Modifier.weight(1f))
            StatCard("📆 دروس مكتملة", "${st.completedDays}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("📚 كلمات تعلمتها", "${app.learned().size}", Modifier.weight(1f))
            StatCard("🎯 متوسط الاختبارات", "$avg%", Modifier.weight(1f))
        }
        StatCard("🎙 تسجيلاتك", "${st.recordings().size}", Modifier.fillMaxWidth())

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰ تذكير يومي", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Switch(checked = on, onCheckedChange = { c ->
                        on = c
                        st.reminderOn = c
                        if (c) {
                            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            Reminder.schedule(ctx, h, m)
                        } else {
                            Reminder.cancel(ctx)
                        }
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الوقت: %02d:%02d".format(h, m), Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        TimePickerDialog(ctx, { _, hh, mm ->
                            st.reminderH = hh
                            st.reminderM = mm
                            h = hh
                            m = mm
                            if (on) Reminder.schedule(ctx, hh, mm)
                        }, h, m, true).show()
                    }) { Text("تغيير الوقت") }
                }
            }
        }

        if (hist.isNotEmpty()) {
            Text("آخر الاختبارات", fontWeight = FontWeight.Bold)
            hist.takeLast(7).reversed().forEach {
                Text("${it[0]}  •  الدرس ${it[1]}  •  ${it[2]}/${it[3]}")
            }
        }
    }
}
