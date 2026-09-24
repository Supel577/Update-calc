package com.example.calcvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VaultMediaType {
    PHOTO,
    VIDEO,
    AUDIO,
    DOCUMENT
}

@Entity(tableName = "vault_media")
data class VaultMedia(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val originalName: String,
    val mediaType: VaultMediaType,
    val sizeBytes: Long,
    val dateAdded: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val relativePath: String = "",
    val inTrash: Boolean = false,
    val deletedTimestamp: Long = 0L
)

@Entity(tableName = "secret_notes")
data class SecretNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val dateModified: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val colorIndex: Int = 0
)
