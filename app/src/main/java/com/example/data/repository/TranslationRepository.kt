package com.example.data.repository

import com.example.data.local.TranslationDao
import com.example.data.model.TranslationHistory
import com.example.data.model.TranslatedPage
import kotlinx.coroutines.flow.Flow

class TranslationRepository(private val translationDao: TranslationDao) {
    val allHistory: Flow<List<TranslationHistory>> = translationDao.getAllHistory()

    fun getPagesForHistory(historyId: Int): Flow<List<TranslatedPage>> {
        return translationDao.getPagesForHistory(historyId)
    }

    suspend fun insertHistory(history: TranslationHistory): Int {
        return translationDao.insertHistory(history).toInt()
    }

    suspend fun insertPage(page: TranslatedPage) {
        translationDao.insertPage(page)
    }

    suspend fun deleteHistory(historyId: Int) {
        translationDao.deleteHistoryById(historyId)
    }

    suspend fun toggleBookmark(historyId: Int, isBookmarked: Boolean) {
        translationDao.updateBookmark(historyId, isBookmarked)
    }

    suspend fun updatePageNotes(pageId: Int, notes: String) {
        translationDao.updatePageNotes(pageId, notes)
    }

    suspend fun clearAllHistory() {
        translationDao.clearAllHistory()
    }
}
