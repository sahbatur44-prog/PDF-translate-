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
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TranslationHistory>>

    @Query("SELECT * FROM translated_pages WHERE translationHistoryId = :historyId ORDER BY pageNumber ASC")
    fun getPagesForHistory(historyId: Int): Flow<List<TranslatedPage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TranslationHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: TranslatedPage)

    @Query("DELETE FROM translation_history WHERE id = :historyId")
    suspend fun deleteHistoryById(historyId: Int)

    @Query("UPDATE translation_history SET isBookmarked = :isBookmarked WHERE id = :historyId")
    suspend fun updateBookmark(historyId: Int, isBookmarked: Boolean)

    @Query("UPDATE translated_pages SET userNotes = :notes WHERE id = :pageId")
    suspend fun updatePageNotes(pageId: Int, notes: String)

    @Query("DELETE FROM translation_history")
    suspend fun clearAllHistory()
}
