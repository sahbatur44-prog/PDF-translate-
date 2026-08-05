package com.example.data.repository

import com.example.data.local.TranslationDao
import com.example.data.model.TranslationHistory
import com.example.data.model.TranslatedPage
import kotlinx.coroutines.flow.Flow

class TranslationRepository(private val translationDao: TranslationDao) {
    fun getAllHistoryForUser(userId: String): Flow<List<TranslationHistory>> {
        return translationDao.getAllHistoryForUser(userId)
    }

    fun getPagesForHistory(historyId: Int): Flow<List<TranslatedPage>> {
        return translationDao.getPagesForHistory(historyId)
    }

    suspend fun getPagesListForHistory(historyId: Int): List<TranslatedPage> {
        return translationDao.getPagesListForHistory(historyId)
    }

    suspend fun getAllPagesListForUser(userId: String): List<TranslatedPage> {
        return translationDao.getAllPagesListForUser(userId)
    }

    suspend fun insertHistory(history: TranslationHistory): Int {
        return translationDao.insertHistory(history).toInt()
    }

    suspend fun insertPage(page: TranslatedPage) {
        translationDao.insertPage(page)
    }

    suspend fun deleteHistoryForUser(historyId: Int, userId: String) {
        translationDao.deleteHistoryByIdForUser(historyId, userId)
    }

    suspend fun toggleBookmark(historyId: Int, isBookmarked: Boolean) {
        translationDao.updateBookmark(historyId, isBookmarked)
    }

    suspend fun updatePageNotes(pageId: Int, notes: String) {
        translationDao.updatePageNotes(pageId, notes)
    }

    suspend fun clearAllHistoryForUser(userId: String) {
        translationDao.clearAllHistoryForUser(userId)
    }
}
