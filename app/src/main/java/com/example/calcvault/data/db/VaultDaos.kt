package com.example.calcvault.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.calcvault.data.model.SecretNote
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.model.VaultMediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM vault_media WHERE mediaType = :type AND inTrash = 0 ORDER BY dateAdded DESC")
    fun getMediaByType(type: VaultMediaType): Flow<List<VaultMedia>>

    @Query("SELECT * FROM vault_media WHERE inTrash = 0 ORDER BY dateAdded DESC")
    fun getAllActiveMedia(): Flow<List<VaultMedia>>

    @Query("SELECT COUNT(*) FROM vault_media WHERE mediaType = :type AND inTrash = 0")
    fun getCountByType(type: VaultMediaType): Flow<Int>

    @Query("SELECT * FROM vault_media WHERE inTrash = 1 ORDER BY deletedTimestamp DESC")
    fun getTrashMedia(): Flow<List<VaultMedia>>

    @Query("SELECT COUNT(*) FROM vault_media WHERE inTrash = 1")
    fun getTrashCount(): Flow<Int>

    @Query("SELECT * FROM vault_media WHERE inTrash = 1")
    suspend fun getTrashList(): List<VaultMedia>

    @Query("SELECT * FROM vault_media")
    suspend fun getAllMediaList(): List<VaultMedia>

    @Query("SELECT * FROM vault_media WHERE id = :id")
    suspend fun getMediaById(id: Long): VaultMedia?

    @Query("SELECT * FROM vault_media WHERE fileName = :fileName LIMIT 1")
    suspend fun getMediaByFileName(fileName: String): VaultMedia?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: VaultMedia): Long

    @Update
    suspend fun updateMedia(media: VaultMedia)

    @Query("UPDATE vault_media SET inTrash = 1, deletedTimestamp = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vault_media SET inTrash = 0, deletedTimestamp = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Delete
    suspend fun deleteMedia(media: VaultMedia)

    @Query("DELETE FROM vault_media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("DELETE FROM vault_media WHERE inTrash = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM vault_media")
    suspend fun clearAllMedia()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM secret_notes ORDER BY isPinned DESC, dateModified DESC")
    fun getAllNotes(): Flow<List<SecretNote>>

    @Query("SELECT COUNT(*) FROM secret_notes")
    fun getNoteCount(): Flow<Int>

    @Query("SELECT * FROM secret_notes")
    suspend fun getAllNotesList(): List<SecretNote>

    @Query("SELECT * FROM secret_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): SecretNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SecretNote): Long

    @Update
    suspend fun updateNote(note: SecretNote)

    @Delete
    suspend fun deleteNote(note: SecretNote)

    @Query("DELETE FROM secret_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM secret_notes")
    suspend fun clearAllNotes()
}
