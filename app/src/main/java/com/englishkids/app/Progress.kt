package com.englishkids.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Menyimpan kemajuan anak dan mengatur level adaptif:
 * - 10 jawaban terakhir dihitung. Akurasi >= 80% -> naik level, <= 40% -> turun level.
 * - Materi yang sering salah muncul lebih sering (lihat QuizMaker).
 */
class Progress(ctx: Context) {
    private val sp = ctx.getSharedPreferences("progress", Context.MODE_PRIVATE)

    var level by mutableIntStateOf(sp.getInt("level", 1)); private set
    var stars by mutableIntStateOf(sp.getInt("stars", 0)); private set
    var streak by mutableIntStateOf(sp.getInt("streak", 0)); private set

    init {
        touchDay()
    }

    private fun touchDay() {
        val today = System.currentTimeMillis() / 86_400_000L
        val last = sp.getLong("lastDay", -1L)
        if (last != today) {
            streak = if (last == today - 1) streak + 1 else 1
            sp.edit().putLong("lastDay", today).putInt("streak", streak).apply()
        }
    }

    private fun stat(item: Item): List<Int> =
        (sp.getString("s_" + item.en, "0,0") ?: "0,0").split(",").map { it.toIntOrNull() ?: 0 }

    fun isWeak(item: Item): Boolean { val s = stat(item); return s[1] > s[0] }
    fun isLearned(item: Item): Boolean = stat(item)[0] >= 2

    /** Mengembalikan +1 jika naik level, -1 jika turun, 0 jika tetap. */
    fun record(item: Item, correct: Boolean): Int {
        val s = stat(item)
        val c = if (correct) s[0] + 1 else s[0]
        val w = if (correct) s[1] else s[1] + 1
        var win = (sp.getString("win", "") ?: "") + (if (correct) "1" else "0")
        if (win.length > 10) win = win.takeLast(10)
        var change = 0
        if (win.length >= 10) {
            val acc = win.count { it == '1' } / 10f
            if (acc >= 0.8f && level < 5) { level += 1; change = 1 }
            else if (acc <= 0.4f && level > 1) { level -= 1; change = -1 }
            if (change != 0) win = ""
        }
        if (correct) stars += 1
        sp.edit()
            .putString("s_" + item.en, "$c,$w")
            .putString("win", win)
            .putInt("level", level)
            .putInt("stars", stars)
            .apply()
        return change
    }

    fun reset() {
        sp.edit().clear().apply()
        level = 1
        stars = 0
        streak = 0
        touchDay()
    }
}
