package com.englishkids.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface Screen {
    data object Home : Screen
    data object Topics : Screen
    data class Learn(val pack: Pack) : Screen
    data class Quiz(val pack: Pack?) : Screen
    data object Chat : Screen
    data object Parent : Screen
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val progress = remember { Progress(ctx) }
    val speaker = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    var packs by remember { mutableStateOf(ContentRepo.load(ctx)) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    val doSync: suspend () -> String = {
        val m = ContentRepo.sync(ctx)
        if (m.startsWith("Materi diperbarui")) packs = ContentRepo.load(ctx)
        m
    }
    LaunchedEffect(Unit) { doSync() }

    fun back() {
        val cur = screen
        screen = when {
            cur is Screen.Learn -> Screen.Topics
            cur is Screen.Quiz && cur.pack != null -> Screen.Topics
            else -> Screen.Home
        }
    }
    BackHandler(enabled = screen != Screen.Home) { back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(KSky, Color.White)))
            .safeDrawingPadding()
    ) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(progress) { screen = it }
            Screen.Topics -> TopicsScreen(packs, progress, { back() }) { screen = it }
            is Screen.Learn -> LearnScreen(s.pack, progress, speaker, { back() }) { screen = Screen.Quiz(s.pack) }
            is Screen.Quiz -> QuizScreen(packs, s.pack, progress, speaker) { back() }
            Screen.Chat -> ChatScreen(packs, progress) { back() }
            Screen.Parent -> ParentScreen(progress, { back() }, doSync)
        }
    }
}

// ---------- komponen umum ----------

@Composable
fun Chip(text: String, bg: Color = Color.White) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(text, fontWeight = FontWeight.Bold, color = KInk, fontSize = 16.sp)
    }
}

@Composable
fun BigButton(
    text: String, emoji: String, color: Color, modifier: Modifier = Modifier,
    emojiSize: TextUnit = 44.sp, onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(28.dp)).background(color).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = emojiSize)
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, right: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Color.White).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Text("⬅️", fontSize = 24.sp) }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = KInk, modifier = Modifier.weight(1f))
        right()
    }
}

// ---------- layar utama ----------

@Composable
fun HomeScreen(progress: Progress, onGo: (Screen) -> Unit) {
    val t = rememberInfiniteTransition(label = "bounce")
    val dy by t.animateFloat(
        0f, -16f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy"
    )
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("⭐ ${progress.stars}", KSun)
            Chip("🔥 ${progress.streak}")
            Spacer(Modifier.weight(1f))
            Chip("Level ${progress.level}")
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable { onGo(Screen.Parent) },
                contentAlignment = Alignment.Center
            ) { Text("⚙️", fontSize = 20.sp) }
        }
        Spacer(Modifier.weight(0.5f))
        Text("🐱", fontSize = 110.sp, modifier = Modifier.offset(y = dy.dp))
        Text("Hi, I'm Mimo!", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = KInk)
        Text("Ayo belajar bahasa Inggris bersamaku!", fontSize = 16.sp, color = KInk, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(0.5f))
        Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigButton("Belajar", "📚", KBlue, Modifier.weight(1f).fillMaxHeight()) { onGo(Screen.Topics) }
            BigButton("Kuis Seru", "🎮", KGreen, Modifier.weight(1f).fillMaxHeight()) { onGo(Screen.Quiz(null)) }
        }
        Spacer(Modifier.height(16.dp))
        BigButton("Ngobrol dengan Mimo", "💬", KPink, Modifier.fillMaxWidth().height(110.dp), 36.sp) { onGo(Screen.Chat) }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------- daftar topik ----------

@Composable
fun TopicsScreen(packs: List<Pack>, progress: Progress, onBack: () -> Unit, onGo: (Screen) -> Unit) {
    val colors = listOf(KBlue, KGreen, KPink, KPurple, KOrange)
    Column(Modifier.fillMaxSize()) {
        TopBar("Pilih Topik", onBack)
        LazyVerticalGrid(
            GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(packs) { i, p ->
                val done = p.items.count { progress.isLearned(it) }
                Column(
                    Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        Modifier.fillMaxWidth().clickable { onGo(Screen.Learn(p)) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(p.emoji, fontSize = 48.sp)
                        Text(
                            p.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            color = KInk, textAlign = TextAlign.Center
                        )
                        Text("$done/${p.items.size} dikuasai", fontSize = 13.sp, color = KInk.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(colors[i % colors.size])
                            .clickable { onGo(Screen.Quiz(p)) }.padding(horizontal = 18.dp, vertical = 8.dp)
                    ) { Text("🎮 Kuis", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ---------- kartu belajar ----------

@Composable
fun LearnScreen(pack: Pack, progress: Progress, speaker: Speaker, onBack: () -> Unit, onQuiz: () -> Unit) {
    val minL = pack.items.minOfOrNull { it.l } ?: 1
    val unlocked = pack.items.filter { it.l <= maxOf(progress.level, minL) }.sortedBy { it.l }
    val locked = pack.items.size - unlocked.size
    var i by remember { mutableIntStateOf(0) }
    val finished = i >= unlocked.size

    Column(Modifier.fillMaxSize()) {
        TopBar("${pack.emoji} ${pack.title}", onBack)
        if (!finished) {
            val item = unlocked[i]
            LaunchedEffect(i) { speaker.say(item.en) }
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(16.dp)
                    .clip(RoundedCornerShape(32.dp)).background(Color.White)
                    .clickable { speaker.say(item.en) }.padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (item.e.isNotBlank()) Text(item.e, fontSize = 100.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    item.en, fontSize = if (item.en.length > 14) 28.sp else 44.sp,
                    fontWeight = FontWeight.ExtraBold, color = KBlue, textAlign = TextAlign.Center
                )
                Text("🔊 ketuk untuk mendengar", fontSize = 14.sp, color = KInk.copy(alpha = 0.6f))
                Spacer(Modifier.height(12.dp))
                Text(item.idn, fontSize = 26.sp, color = KInk, textAlign = TextAlign.Center)
                if (item.ex.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(item.ex, fontSize = 18.sp, color = KInk.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            }
            Text(
                "${i + 1} / ${unlocked.size}", Modifier.align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold, color = KInk
            )
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("Kembali", "⬅️", KPurple, Modifier.weight(1f).height(84.dp), 26.sp) { if (i > 0) i-- }
                BigButton("Lanjut", "➡️", KGreen, Modifier.weight(1f).height(84.dp), 26.sp) { i++ }
            }
        } else {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🎉", fontSize = 90.sp)
                Text("Hebat! Semua kartu sudah dilihat.", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = KInk, textAlign = TextAlign.Center)
                if (locked > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Ada $locked materi baru yang terbuka saat levelmu naik.", color = KInk, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(24.dp))
                BigButton("Coba Kuis", "🎮", KGreen, Modifier.fillMaxWidth().height(90.dp), 30.sp) { onQuiz() }
                Spacer(Modifier.height(12.dp))
                BigButton("Ulangi", "🔁", KBlue, Modifier.fillMaxWidth().height(90.dp), 30.sp) { i = 0 }
            }
        }
    }
}

// ---------- kuis adaptif ----------

@Composable
fun OptionButton(text: String, state: Int, onClick: () -> Unit) {
    val bg = when (state) { 1 -> KGreen; 2 -> KRed; else -> Color.White }
    val fg = if (state == 0) KInk else Color.White
    Box(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(20.dp))
            .background(bg).clickable(onClick = onClick).padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = fg, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizScreen(packs: List<Pack>, only: Pack?, progress: Progress, speaker: Speaker, onExit: () -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf(QuizMaker.make(packs, progress.level, progress, only)) }
    var picked by remember { mutableStateOf<String?>(null) }
    var levelMsg by remember { mutableStateOf("") }
    val built = remember { mutableStateListOf<Int>() }

    fun submit(answer: String) {
        val cur = q ?: return
        if (picked != null) return
        picked = answer
        val ok = answer == cur.answer
        if (ok) correctCount += 1
        val ch = progress.record(cur.item, ok)
        levelMsg = when {
            ch > 0 -> "🎉 Naik ke Level ${progress.level}!"
            ch < 0 -> "Ayo ulang dulu, sekarang Level ${progress.level}"
            else -> ""
        }
        speaker.say(cur.item.en)
    }

    fun next() {
        round += 1
        picked = null
        built.clear()
        levelMsg = ""
        q = QuizMaker.make(packs, progress.level, progress, only)
    }

    fun restart() {
        round = 0
        correctCount = 0
        picked = null
        built.clear()
        levelMsg = ""
        q = QuizMaker.make(packs, progress.level, progress, only)
    }

    LaunchedEffect(q) {
        val c = q
        if (c != null && c.type == QType.LISTEN) speaker.say(c.item.en)
    }

    val cur = q
    Column(Modifier.fillMaxSize()) {
        TopBar("Kuis", onExit) { Chip("Level ${progress.level}", KSun) }
        if (round >= 10) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (correctCount >= 8) "🏆" else if (correctCount >= 5) "👍" else "💪", fontSize = 100.sp)
                Text("Benar $correctCount dari 10", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = KInk)
                Text("⭐ ${progress.stars} bintang", fontSize = 20.sp, color = KInk)
                Spacer(Modifier.height(24.dp))
                BigButton("Main Lagi", "🔁", KGreen, Modifier.fillMaxWidth().height(90.dp), 30.sp) { restart() }
                Spacer(Modifier.height(12.dp))
                BigButton("Selesai", "🏠", KBlue, Modifier.fillMaxWidth().height(90.dp), 30.sp) { onExit() }
            }
        } else if (cur == null) {
            Text("Belum ada materi.", Modifier.padding(24.dp), color = KInk)
        } else {
            LinearProgressIndicator(
                progress = { round / 10f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(10.dp).clip(RoundedCornerShape(50)),
                color = KGreen
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (cur.type) {
                    QType.MEANING -> {
                        Text("Apa artinya?", fontSize = 18.sp, color = KInk)
                        Row(
                            Modifier.clickable { speaker.say(cur.item.en) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                cur.item.en, fontSize = if (cur.item.en.length > 14) 24.sp else 40.sp,
                                fontWeight = FontWeight.ExtraBold, color = KBlue, modifier = Modifier.weight(1f, false)
                            )
                            Text(" 🔊", fontSize = 28.sp)
                        }
                    }
                    QType.PICTURE -> {
                        Text("Bahasa Inggrisnya apa?", fontSize = 18.sp, color = KInk)
                        Text(cur.item.e, fontSize = 100.sp)
                    }
                    QType.LISTEN -> {
                        Text("Dengarkan, lalu pilih jawabannya", fontSize = 18.sp, color = KInk)
                        Box(
                            Modifier.size(96.dp).clip(CircleShape).background(KSun).clickable { speaker.say(cur.item.en) },
                            contentAlignment = Alignment.Center
                        ) { Text("🔊", fontSize = 44.sp) }
                    }
                    QType.SCRAMBLE -> {
                        Text("Susun dalam bahasa Inggris:", fontSize = 18.sp, color = KInk)
                        Text(cur.item.idn, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = KInk, textAlign = TextAlign.Center)
                    }
                }

                if (cur.type != QType.SCRAMBLE) {
                    cur.options.forEach { opt ->
                        val st = when {
                            picked == null -> 0
                            opt == cur.answer -> 1
                            opt == picked -> 2
                            else -> 0
                        }
                        OptionButton(opt, st) { submit(opt) }
                    }
                } else {
                    FlowRow(
                        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(20.dp))
                            .background(Color.White).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        built.forEach { idx ->
                            Box(
                                Modifier.clip(RoundedCornerShape(14.dp)).background(KBlue)
                                    .clickable { if (picked == null) built.remove(idx) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) { Text(cur.tokens[idx], fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cur.tokens.forEachIndexed { idx, tk ->
                            if (idx !in built) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(14.dp)).background(KSun)
                                        .clickable { if (picked == null) built.add(idx) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) { Text(tk, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KInk) }
                            }
                        }
                    }
                    if (built.size == cur.tokens.size && picked == null) {
                        Button(
                            onClick = { submit(built.joinToString(cur.joiner) { cur.tokens[it] }) },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) { Text("Periksa ✅", fontSize = 20.sp) }
                    }
                }

                if (picked != null) {
                    val ok = picked == cur.answer
                    Text(
                        if (ok) "✅ Hebat!" else "❌ Jawaban benar: ${cur.answer}",
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (ok) KGreen else KRed, textAlign = TextAlign.Center
                    )
                    if (levelMsg.isNotEmpty()) {
                        Text(levelMsg, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KPurple, textAlign = TextAlign.Center)
                    }
                    Button(onClick = { next() }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        Text("Lanjut ➡️", fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

// ---------- ngobrol dengan Mimo ----------

@Composable
fun ChatScreen(packs: List<Pack>, progress: Progress, onBack: () -> Unit) {
    val msgs: SnapshotStateList<ChatMsg> = remember {
        mutableStateListOf(ChatMsg(false, "Hello! I am Mimo 🐱 Let us talk in English! What is your name?"))
    }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(msgs.size) { listState.animateScrollToItem(msgs.lastIndex) }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || sending) return
        msgs.add(ChatMsg(true, t))
        input = ""
        sending = true
        scope.launch {
            val r = Mimo.reply(msgs.toList(), progress.level, packs)
            msgs.add(ChatMsg(false, r))
            sending = false
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar("Ngobrol dengan Mimo", onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(msgs.size) { idx ->
                val m = msgs[idx]
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromChild) Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(20.dp))
                            .background(if (m.fromChild) KBlue else Color.White).padding(12.dp)
                    ) {
                        Text(
                            (if (m.fromChild) "" else "🐱 ") + m.text,
                            fontSize = 18.sp, color = if (m.fromChild) Color.White else KInk
                        )
                    }
                }
            }
        }
        if (sending) Text("Mimo sedang mengetik… 🐱", Modifier.padding(horizontal = 16.dp), color = KInk)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Hello!", "How are you?", "I like cats.", "What is your name?").forEach { s ->
                Box(Modifier.clickable { send(s) }) { Chip(s, KSun) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Tulis dalam bahasa Inggris…") },
                singleLine = true, shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(KBlue).clickable { send(input) },
                contentAlignment = Alignment.Center
            ) { Text("➤", fontSize = 24.sp, color = Color.White) }
        }
    }
}

// ---------- area orang tua ----------

@Composable
fun ParentScreen(progress: Progress, onBack: () -> Unit, onSync: suspend () -> String) {
    val a = remember { Random.nextInt(3, 9) }
    val b = remember { Random.nextInt(3, 9) }
    var ans by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        TopBar("Area Orang Tua", onBack)
        Column(
            Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!open) {
                Text("Untuk masuk, jawab: $a × $b = ?", fontSize = 20.sp, color = KInk)
                OutlinedTextField(
                    value = ans,
                    onValueChange = { ans = it; if (it.trim() == (a * b).toString()) open = true },
                    singleLine = true
                )
            } else {
                Text("Versi materi: v${ContentRepo.version(ctx)}", fontSize = 18.sp, color = KInk)
                Text(
                    if (BuildConfig.AI_PROXY_URL.isNotBlank()) "Teman AI: online" else "Teman AI: mode offline (server AI belum diatur)",
                    fontSize = 16.sp, color = KInk
                )
                Text(
                    "Level otomatis naik jika 8 dari 10 jawaban terakhir benar, dan turun jika kurang dari 5.",
                    fontSize = 14.sp, color = KInk.copy(alpha = 0.7f)
                )
                Button(
                    onClick = { scope.launch { msg = "Memeriksa…"; msg = onSync() } },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("🔄 Perbarui materi sekarang") }
                if (msg.isNotEmpty()) Text(msg, color = KInk)
                OutlinedButton(
                    onClick = { progress.reset(); msg = "Progres belajar direset." },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("🗑️ Reset progres belajar") }
            }
        }
    }
}
