package com.example.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GoogleTranslateClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun translateText(text: String, sourceLang: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""

        val sl = mapLanguageCode(sourceLang)
        val tl = mapLanguageCode(targetLang)

        // Split long text into chunks if needed (Google Translate free endpoint accepts ~1800 chars per query)
        val chunks = text.split("\n\n").flatMap { paragraph ->
            if (paragraph.length > 1500) {
                paragraph.chunked(1500)
            } else {
                listOf(paragraph)
            }
        }

        val translatedChunks = mutableListOf<String>()

        for (chunk in chunks) {
            if (chunk.isBlank()) {
                translatedChunks.add("")
                continue
            }
            try {
                val encodedQuery = URLEncoder.encode(chunk, "UTF-8")
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=$tl&dt=t&q=$encodedQuery"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("GoogleTranslateClient", "HTTP error ${response.code}")
                        translatedChunks.add(chunk)
                        return@use
                    }
                    val responseBody = response.body?.string() ?: ""
                    val jsonArray = JSONArray(responseBody)
                    val sentencesArray = jsonArray.optJSONArray(0)
                    val resultSb = StringBuilder()
                    if (sentencesArray != null) {
                        for (i in 0 until sentencesArray.length()) {
                            val sentence = sentencesArray.optJSONArray(i)
                            if (sentence != null) {
                                val translatedSentence = sentence.optString(0, "")
                                resultSb.append(translatedSentence)
                            }
                        }
                    }
                    val finalResult = resultSb.toString()
                    translatedChunks.add(if (finalResult.isNotBlank()) finalResult else chunk)
                }
            } catch (e: Exception) {
                Log.e("GoogleTranslateClient", "Translation exception: ${e.message}", e)
                translatedChunks.add(chunk)
            }
        }

        translatedChunks.joinToString("\n\n")
    }

    fun mapLanguageCode(langName: String): String {
        return when (langName.lowercase()) {
            "otomatik algıla", "auto-detect", "auto" -> "auto"
            "ingilizce", "english" -> "en"
            "türkçe", "turkish" -> "tr"
            "almanca", "german" -> "de"
            "fransızca", "french" -> "fr"
            "ispanyolca", "spanish" -> "es"
            "i̇talyanca", "italyanca", "italian" -> "it"
            "rusça", "russian" -> "ru"
            "arapça", "arabic" -> "ar"
            "çince", "chinese" -> "zh-CN"
            "japonca", "japanese" -> "ja"
            "korece", "korean" -> "ko"
            else -> "auto"
        }
    }
}
