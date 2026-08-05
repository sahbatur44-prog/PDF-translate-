package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TranslationHistory
import com.example.data.model.TranslatedPage
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translation_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHistoryForUser(userId: String): Flow<List<TranslationHistory>>

    @Query("SELECT * FROM translated_pages WHERE translationHistoryId = :historyId ORDER BY pageNumber ASC")
    fun getPagesForHistory(historyId: Int): Flow<List<TranslatedPage>>

    @Query("SELECT * FROM translated_pages WHERE translationHistoryId = :historyId")
    suspend fun getPagesListForHistory(historyId: Int): List<TranslatedPage>

    @Query("SELECT tp.* FROM translated_pages tp INNER JOIN translation_history th ON tp.translationHistoryId = th.id WHERE th.userId = :userId")
    suspend fun getAllPagesListForUser(userId: String): List<TranslatedPage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TranslationHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: TranslatedPage)

    @Query("DELETE FROM translation_history WHERE id = :historyId AND userId = :userId")
    suspend fun deleteHistoryByIdForUser(historyId: Int, userId: String)

    @Query("UPDATE translation_history SET isBookmarked = :isBookmarked WHERE id = :historyId")
    suspend fun updateBookmark(historyId: Int, isBookmarked: Boolean)

    @Query("UPDATE translated_pages SET userNotes = :notes WHERE id = :pageId")
    suspend fun updatePageNotes(pageId: Int, notes: String)

    @Query("DELETE FROM translation_history WHERE userId = :userId")
    suspend fun clearAllHistoryForUser(userId: String)
}
