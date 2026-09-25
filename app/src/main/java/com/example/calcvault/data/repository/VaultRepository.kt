package com.example.calcvault.data.repository

import android.net.Uri
import com.example.calcvault.data.db.MediaDao
import com.example.calcvault.data.db.NoteDao
import com.example.calcvault.data.model.SecretNote
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.model.VaultMediaType
import com.example.calcvault.data.security.VaultSecurityManager
import com.example.calcvault.data.storage.VaultFileManager
import kotlinx.coroutines.flow.Flow
import java.io.File

class VaultRepository(
    private val mediaDao: MediaDao,
    private val noteDao: NoteDao,
    val fileManager: VaultFileManager,
    val securityManager: VaultSecurityManager
) {
    val hiddenPhotos: Flow<List<VaultMedia>> = mediaDao.getMediaByType(VaultMediaType.PHOTO)
    val hiddenVideos: Flow<List<VaultMedia>> = mediaDao.getMediaByType(VaultMediaType.VIDEO)
    val hiddenAudios: Flow<List<VaultMedia>> = mediaDao.getMediaByType(VaultMediaType.AUDIO)
    val hiddenDocuments: Flow<List<VaultMedia>> = mediaDao.getMediaByType(VaultMediaType.DOCUMENT)
    val trashMedia: Flow<List<VaultMedia>> = mediaDao.getTrashMedia()
    val allNotes: Flow<List<SecretNote>> = noteDao.getAllNotes()

    val photoCount: Flow<Int> = mediaDao.getCountByType(VaultMediaType.PHOTO)
    val videoCount: Flow<Int> = mediaDao.getCountByType(VaultMediaType.VIDEO)
    val audioCount: Flow<Int> = mediaDao.getCountByType(VaultMediaType.AUDIO)
    val documentCount: Flow<Int> = mediaDao.getCountByType(VaultMediaType.DOCUMENT)
    val trashCount: Flow<Int> = mediaDao.getTrashCount()
    val noteCount: Flow<Int> = noteDao.getNoteCount()

    suspend fun importMedia(uri: Uri, isVideo: Boolean, isAudio: Boolean = false): Boolean {
        val vaultMedia = fileManager.importMedia(uri, isVideo, isAudio) ?: return false
        val id = mediaDao.insertMedia(vaultMedia)
        return id > 0
    }

    suspend fun importDocument(uri: Uri): Boolean {
        val vaultMedia = fileManager.importDocument(uri) ?: return false
        val id = mediaDao.insertMedia(vaultMedia)
        return id > 0
    }

    suspend fun saveSecretCameraPhoto(tempFile: java.io.File): VaultMedia? {
        val vaultMedia = fileManager.saveSecretCameraPhoto(tempFile) ?: return null
        val id = mediaDao.insertMedia(vaultMedia)
        return if (id > 0) vaultMedia.copy(id = id) else null
    }

    suspend fun saveSecretCameraVideo(tempFile: java.io.File): VaultMedia? {
        val vaultMedia = fileManager.saveSecretCameraVideo(tempFile) ?: return null
        val id = mediaDao.insertMedia(vaultMedia)
        return if (id > 0) vaultMedia.copy(id = id) else null
    }

    suspend fun saveSecretRecordedAudio(tempFile: java.io.File, customTitle: String? = null): VaultMedia? {
        val vaultMedia = fileManager.saveSecretRecordedAudio(tempFile, customTitle) ?: return null
        val id = mediaDao.insertMedia(vaultMedia)
        return if (id > 0) vaultMedia.copy(id = id) else null
    }

    suspend fun moveToTrash(media: VaultMedia) {
        mediaDao.moveToTrash(media.id)
    }

    suspend fun restoreFromTrash(media: VaultMedia) {
        mediaDao.restoreFromTrash(media.id)
    }

    suspend fun permanentDelete(media: VaultMedia): Boolean {
        fileManager.deleteMediaFile(media)
        mediaDao.deleteMedia(media)
        return true
    }

    suspend fun emptyTrash() {
        val trashItems = mediaDao.getTrashList()
        for (item in trashItems) {
            fileManager.deleteMediaFile(item)
            mediaDao.deleteMedia(item)
        }
    }

    suspend fun batchMoveToTrash(mediaList: List<VaultMedia>) {
        for (media in mediaList) {
            mediaDao.moveToTrash(media.id)
        }
    }

    suspend fun batchRestoreFromTrash(mediaList: List<VaultMedia>) {
        for (media in mediaList) {
            mediaDao.restoreFromTrash(media.id)
        }
    }

    suspend fun batchPermanentDelete(mediaList: List<VaultMedia>) {
        for (media in mediaList) {
            fileManager.deleteMediaFile(media)
            mediaDao.deleteMedia(media)
        }
    }

    suspend fun reindexPersistentFiles() {
        // Automatically check and index any files present in storage directory across uninstalls
        try {
            val existingMedia = mediaDao.getAllMediaList()
            val existingFileNames = existingMedia.map { it.fileName }.toSet()
            val onDiskFiles = fileManager.getDiscoveredFiles()
            for (file in onDiskFiles) {
                if (!existingFileNames.contains(file.fileName)) {
                    mediaDao.insertMedia(file)
                }
            }

            // Also restore notes if Room database was cleared by app uninstall
            val currentNotes = noteDao.getAllNotesList()
            if (currentNotes.isEmpty()) {
                val backupJson = fileManager.readNotesBackup()
                if (!backupJson.isNullOrEmpty()) {
                    val array = org.json.JSONArray(backupJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val note = SecretNote(
                            title = obj.optString("title", ""),
                            content = obj.optString("content", ""),
                            dateModified = obj.optLong("dateModified", System.currentTimeMillis()),
                            isPinned = obj.optBoolean("isPinned", false),
                            colorIndex = obj.optInt("colorIndex", 0)
                        )
                        noteDao.insertNote(note)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun syncNotesBackup() {
        try {
            val notes = noteDao.getAllNotesList()
            val array = org.json.JSONArray()
            for (note in notes) {
                val obj = org.json.JSONObject().apply {
                    put("title", note.title)
                    put("content", note.content)
                    put("dateModified", note.dateModified)
                    put("isPinned", note.isPinned)
                    put("colorIndex", note.colorIndex)
                }
                array.put(obj)
            }
            fileManager.saveNotesBackup(array.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun downloadMediaDirectToVault(url: String, contentDisposition: String?, mimeType: String?): VaultMedia? {
        val vaultMedia = fileManager.downloadMediaDirectly(url, contentDisposition, mimeType) ?: return null
        val id = mediaDao.insertMedia(vaultMedia)
        return if (id > 0) vaultMedia.copy(id = id) else null
    }

    suspend fun saveScreenshotToVault(bitmap: android.graphics.Bitmap, title: String = "Secret_Screenshot"): VaultMedia? {
        val vaultMedia = fileManager.saveBitmapToVault(bitmap, title) ?: return null
        val id = mediaDao.insertMedia(vaultMedia)
        return if (id > 0) vaultMedia.copy(id = id) else null
    }

    suspend fun deleteMedia(media: VaultMedia): Boolean {
        fileManager.deleteMediaFile(media)
        mediaDao.deleteMedia(media)
        return true
    }

    suspend fun unhideMedia(media: VaultMedia): Boolean {
        val exported = fileManager.exportBackToPublicGallery(media)
        if (exported) {
            fileManager.deleteMediaFile(media)
            mediaDao.deleteMedia(media)
            return true
        }
        return false
    }

    fun getMediaFile(media: VaultMedia): File {
        return fileManager.getFile(media)
    }

    suspend fun saveNote(title: String, content: String, id: Long = 0, isPinned: Boolean = false, colorIndex: Int = 0): Long {
        val note = SecretNote(
            id = id,
            title = title.trim(),
            content = content.trim(),
            dateModified = System.currentTimeMillis(),
            isPinned = isPinned,
            colorIndex = colorIndex
        )
        val resultId = if (id == 0L) {
            noteDao.insertNote(note)
        } else {
            noteDao.updateNote(note)
            id
        }
        syncNotesBackup()
        return resultId
    }

    suspend fun deleteNote(note: SecretNote) {
        noteDao.deleteNote(note)
        syncNotesBackup()
    }

    suspend fun togglePinNote(note: SecretNote) {
        noteDao.updateNote(note.copy(isPinned = !note.isPinned, dateModified = System.currentTimeMillis()))
        syncNotesBackup()
    }
}
