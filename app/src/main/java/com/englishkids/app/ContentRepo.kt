package com.englishkids.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Materi dibaca dari cache hasil unduhan (jika lebih baru) atau dari aset bawaan APK.
 * Ubah file di folder content/ di GitHub -> naikkan "version" di index.json -> aplikasi
 * anak otomatis mengunduh materi baru tanpa perlu update APK.
 */
object ContentRepo {
    private fun cacheDir(ctx: Context) = File(ctx.filesDir, "content")

    private fun readAsset(ctx: Context, name: String): String? =
        try { ctx.assets.open(name).bufferedReader().use { it.readText() } } catch (e: Exception) { null }

    private fun readCache(ctx: Context, name: String): String? {
        val f = File(cacheDir(ctx), name)
        return if (f.exists()) f.readText() else null
    }

    private fun writeCache(ctx: Context, name: String, text: String) {
        val f = File(cacheDir(ctx), name)
        f.parentFile?.mkdirs()
        f.writeText(text)
    }

    private fun parseIndex(text: String?): ContentIndex? =
        try { if (text == null) null else AppJson.decodeFromString<ContentIndex>(text) } catch (e: Exception) { null }

    fun version(ctx: Context): Int {
        val a = parseIndex(readAsset(ctx, "index.json"))?.version ?: 0
        val c = parseIndex(readCache(ctx, "index.json"))?.version ?: 0
        return maxOf(a, c)
    }

    fun load(ctx: Context): List<Pack> {
        val assetIdx = parseIndex(readAsset(ctx, "index.json"))
        val cacheIdx = parseIndex(readCache(ctx, "index.json"))
        val useCache = cacheIdx != null && (assetIdx == null || cacheIdx.version >= assetIdx.version)
        val idx = (if (useCache) cacheIdx else assetIdx) ?: return emptyList()
        return idx.packs.mapNotNull { name ->
            val text = (if (useCache) readCache(ctx, name) else null) ?: readAsset(ctx, name)
            try { text?.let { AppJson.decodeFromString<Pack>(it) } } catch (e: Exception) { null }
        }
    }

    private fun httpGet(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 15000
        try {
            if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** Mengembalikan pesan status untuk ditampilkan di Area Orang Tua. */
    suspend fun sync(ctx: Context): String = withContext(Dispatchers.IO) {
        val base = BuildConfig.CONTENT_BASE_URL
        if (base.isBlank()) return@withContext "Alamat materi online belum diatur."
        try {
            val remoteText = httpGet(base + "index.json")
            val remote = AppJson.decodeFromString<ContentIndex>(remoteText)
            val local = version(ctx)
            if (remote.version <= local) return@withContext "Materi sudah terbaru (v$local)."
            val downloaded = remote.packs.map { it to httpGet(base + it) }
            downloaded.forEach { AppJson.decodeFromString<Pack>(it.second) } // validasi dulu
            downloaded.forEach { (name, text) -> writeCache(ctx, name, text) }
            writeCache(ctx, "index.json", remoteText)
            "Materi diperbarui ke v${remote.version}."
        } catch (e: Exception) {
            "Gagal memperbarui materi (cek internet)."
        }
    }
}
