package com.englishkids.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

data class ChatMsg(val fromChild: Boolean, val text: String)

/** Teman ngobrol: pakai AI online (lewat proxy) jika tersedia, jika tidak pakai mode offline. */
object Mimo {
    suspend fun reply(history: List<ChatMsg>, level: Int, packs: List<Pack>): String {
        val url = BuildConfig.AI_PROXY_URL
        if (url.isNotBlank()) {
            try {
                return remote(url, history, level)
            } catch (e: Exception) {
                // jatuh ke mode offline
            }
        }
        return local(history.lastOrNull()?.text ?: "", level, packs)
    }

    private suspend fun remote(url: String, history: List<ChatMsg>, level: Int): String =
        withContext(Dispatchers.IO) {
            val msgs = JsonArray(
                history.takeLast(12).dropWhile { !it.fromChild }.map { m ->
                    buildJsonObject {
                        put("role", if (m.fromChild) "user" else "assistant")
                        put("content", m.text)
                    }
                }
            )
            val body = buildJsonObject {
                put("level", level)
                put("messages", msgs)
            }.toString()
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.requestMethod = "POST"
                c.connectTimeout = 10000
                c.readTimeout = 30000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
                if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
                val text = c.inputStream.bufferedReader().use { it.readText() }
                AppJson.parseToJsonElement(text).jsonObject["reply"]!!.jsonPrimitive.content
            } finally {
                c.disconnect()
            }
        }

    private fun local(text: String, level: Int, packs: List<Pack>): String {
        val t = text.trim().lowercase()
        val words = t.split(Regex("[^a-z']+")).filter { it.isNotBlank() }
        val all = packs.flatMap { it.items }
        val hit = all.firstOrNull { it.en.lowercase() in words }
        val hint = level <= 2
        return when {
            words.any { it in listOf("hello", "hi", "hey") } ->
                "Hello! I am Mimo 🐱 Nice to meet you!" + (if (hint) " (Halo! Senang bertemu denganmu!)" else "") + " How are you?"
            "how are you" in t ->
                "I am great, thank you! 😺" + (if (hint) " (Aku baik, terima kasih!)" else "") + " What is your favorite animal?"
            "your name" in t -> "My name is Mimo! What is your name?"
            hit != null ->
                "Great word! '${hit.en}' means '${hit.idn}' ${hit.e}. Can you make a sentence with '${hit.en}'?"
            else -> {
                val pick = all.filter { it.l <= level }.randomOrNull()
                if (pick == null) "Let us learn English together! 🐱"
                else "Good try! Do you know this word? '${pick.en}' ${pick.e} Try to use it in a sentence!"
            }
        }
    }
}
