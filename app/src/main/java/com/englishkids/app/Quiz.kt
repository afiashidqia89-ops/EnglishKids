package com.englishkids.app

import kotlin.math.abs
import kotlin.random.Random

enum class QType { MEANING, PICTURE, LISTEN, SCRAMBLE }

data class Question(
    val item: Item,
    val type: QType,
    val options: List<String> = emptyList(),
    val answer: String,
    val tokens: List<String> = emptyList(),
    val joiner: String = ""
)

object QuizMaker {
    fun make(packs: List<Pack>, level: Int, progress: Progress, only: Pack? = null): Question? {
        val source = if (only != null) listOf(only) else packs
        val all = source.flatMap { p -> p.items.map { p to it } }
        val pool = all.filter { it.second.l <= level }.ifEmpty { all }
        if (pool.isEmpty()) return null

        // materi level sekarang & materi yang sering salah lebih sering muncul
        val weights = pool.map { (_, i) -> (if (i.l == level) 3 else 1) + (if (progress.isWeak(i)) 2 else 0) }
        var r = Random.nextInt(weights.sum())
        var idx = 0
        for (k in weights.indices) {
            r -= weights[k]
            if (r < 0) { idx = k; break }
        }
        val (pack, item) = pool[idx]
        val phrase = pack.kind == "phrase"
        val same = packs.filter { it.kind == pack.kind }.flatMap { it.items }.filter { it.en != item.en }

        fun distract(pick: (Item) -> String): List<String> {
            val correct = pick(item)
            return same.shuffled().sortedBy { abs(it.l - item.l) }
                .map(pick).distinct().filter { it != correct }.take(3)
        }

        val types = mutableListOf(QType.MEANING, QType.LISTEN)
        if (!phrase && item.e.isNotBlank()) types.add(QType.PICTURE)
        val scramblable = if (phrase) item.en.split(" ").size >= 2
        else (item.en.length in 3..9 && !item.en.contains(' '))
        if (scramblable) types.add(QType.SCRAMBLE)

        return when (types.random()) {
            QType.MEANING -> Question(
                item, QType.MEANING,
                options = (listOf(item.idn) + distract { it.idn }).shuffled(), answer = item.idn
            )
            QType.PICTURE -> Question(
                item, QType.PICTURE,
                options = (listOf(item.en) + distract { it.en }).shuffled(), answer = item.en
            )
            QType.LISTEN ->
                if (phrase) Question(
                    item, QType.LISTEN,
                    options = (listOf(item.idn) + distract { it.idn }).shuffled(), answer = item.idn
                ) else Question(
                    item, QType.LISTEN,
                    options = (listOf(item.en) + distract { it.en }).shuffled(), answer = item.en
                )
            QType.SCRAMBLE -> {
                val tokens = if (phrase) item.en.split(" ") else item.en.map { it.toString() }
                var t = tokens.shuffled()
                var n = 0
                while (t == tokens && n++ < 5) t = t.shuffled()
                Question(item, QType.SCRAMBLE, answer = item.en, tokens = t, joiner = if (phrase) " " else "")
            }
        }
    }
}
