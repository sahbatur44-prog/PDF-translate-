package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "translation_history")
data class TranslationHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long = System.currentTimeMillis(),
    val documentType: String = "Standart Belge", // e.g. "Standart Belge", "Çizgi Roman / Manga", "Akademik Makale", "Teknik Kılavuz"
    val formality: String = "Resmi", // e.g. "Resmi", "Samimi"
    val isBookmarked: Boolean = false
)

@Entity(
    tableName = "translated_pages",
    foreignKeys = [
        ForeignKey(
            entity = TranslationHistory::class,
            parentColumns = ["id"],
            childColumns = ["translationHistoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["translationHistoryId"])]
)
data class TranslatedPage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val translationHistoryId: Int,
    val pageNumber: Int,
    val originalPagePath: String?, // Absolute file path to the cached original page bitmap
    val translatedText: String,
    val confidenceScore: String = "100%", // From Gemini
    val keyVocabulary: String = "",       // Comma separated terms
    val userNotes: String = ""            // Custom notes by the user
)
