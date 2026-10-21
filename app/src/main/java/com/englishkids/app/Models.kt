package com.englishkids.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val AppJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Satu materi: kata atau kalimat. l = level kesulitan 1..5 */
@Serializable
data class Item(
    val en: String,
    @SerialName("id") val idn: String,
    val e: String = "",
    val l: Int = 1,
    val ex: String = ""
)

/** Satu paket topik. kind = "word" atau "phrase" */
@Serializable
data class Pack(
    val id: String,
    val title: String,
    val emoji: String = "📚",
    val kind: String = "word",
    val items: List<Item> = emptyList()
)

@Serializable
data class ContentIndex(val version: Int = 0, val packs: List<String> = emptyList())
