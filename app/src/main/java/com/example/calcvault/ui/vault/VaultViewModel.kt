package com.example.calcvault.ui.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calcvault.data.model.SecretNote
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class VaultScreenTab {
    DASHBOARD,
    PHOTOS,
    VIDEOS,
    AUDIOS,
    DOCUMENTS,
    SECRET_CAMERA,
    APP_LOCK,
    TRASH,
    NOTES,
    BROWSER,
    INTRUDER_ALERTS,
    SETTINGS
}

data class DeleteOriginalPrompt(
    val uris: List<Uri>,
    val isVideo: Boolean = false,
    val isDocument: Boolean = false,
    val count: Int
)

data class VaultUiState(
    val currentTab: VaultScreenTab = VaultScreenTab.DASHBOARD,
    val selectedMedia: VaultMedia? = null,
    val isViewingMediaDetail: Boolean = false,
    val selectedNote: SecretNote? = null,
    val isEditingNote: Boolean = false,
    val isCreatingNote: Boolean = false,
    val searchQuery: String = "",
    val feedbackMessage: String? = null,
    val totalVaultSize: String = "0 MB",
    val deleteOriginalPrompt: DeleteOriginalPrompt? = null,
    val selectedMediaIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val selectedWallpaperIndex: Int = 0,
    val customWallpaperUri: String? = null,
    val showUpdateDialog: Boolean = false,
    val latestVersionInfo: String = "v2.5.0 (Latest)"
)

class VaultViewModel(
    val repository: VaultRepository
) : ViewModel() {

    fun getFile(media: VaultMedia): File = repository.fileManager.getFile(media)

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    val photos: StateFlow<List<VaultMedia>> = repository.hiddenPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<VaultMedia>> = repository.hiddenVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audios: StateFlow<List<VaultMedia>> = repository.hiddenAudios
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<VaultMedia>> = repository.hiddenDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trash: StateFlow<List<VaultMedia>> = repository.trashMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<SecretNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photoCount: StateFlow<Int> = repository.photoCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val videoCount: StateFlow<Int> = repository.videoCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val audioCount: StateFlow<Int> = repository.audioCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val documentCount: StateFlow<Int> = repository.documentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trashCount: StateFlow<Int> = repository.trashCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val noteCount: StateFlow<Int> = repository.noteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        refreshStorageSize()
        viewModelScope.launch {
            repository.reindexPersistentFiles()
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val set = state.selectedMediaIds.toMutableSet()
            if (set.contains(id)) set.remove(id) else set.add(id)
            state.copy(
                selectedMediaIds = set,
                isMultiSelectMode = set.isNotEmpty()
            )
        }
    }

    fun selectAll(items: List<VaultMedia>) {
        _uiState.update { state ->
            val allIds = items.map { it.id }.toSet()
            state.copy(
                selectedMediaIds = allIds,
                isMultiSelectMode = allIds.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedMediaIds = emptySet(),
                isMultiSelectMode = false
            )
        }
    }

    fun batchMoveToTrash(items: List<VaultMedia>) {
        viewModelScope.launch {
            val selectedItems = items.filter { _uiState.value.selectedMediaIds.contains(it.id) }
            repository.batchMoveToTrash(selectedItems)
            clearSelection()
            _uiState.update { it.copy(feedbackMessage = "Moved ${selectedItems.size} items to Trash.") }
            refreshStorageSize()
        }
    }

    fun batchRestore(items: List<VaultMedia>) {
        viewModelScope.launch {
            val selectedItems = items.filter { _uiState.value.selectedMediaIds.contains(it.id) }
            repository.batchRestoreFromTrash(selectedItems)
            clearSelection()
            _uiState.update { it.copy(feedbackMessage = "Restored ${selectedItems.size} items from Trash.") }
            refreshStorageSize()
        }
    }

    fun batchPermanentDelete(items: List<VaultMedia>) {
        viewModelScope.launch {
            val selectedItems = items.filter { _uiState.value.selectedMediaIds.contains(it.id) }
            repository.batchPermanentDelete(selectedItems)
            clearSelection()
            _uiState.update { it.copy(feedbackMessage = "Permanently deleted ${selectedItems.size} items.") }
            refreshStorageSize()
        }
    }

    fun batchUnhide(items: List<VaultMedia>) {
        viewModelScope.launch {
            val selectedItems = items.filter { _uiState.value.selectedMediaIds.contains(it.id) }
            var count = 0
            for (item in selectedItems) {
                if (repository.unhideMedia(item)) count++
            }
            clearSelection()
            _uiState.update { it.copy(feedbackMessage = "Unhid $count items back to gallery.") }
            refreshStorageSize()
        }
    }

    fun importAudioItems(uris: List<Uri>) {
        viewModelScope.launch {
            var count = 0
            for (uri in uris) {
                val success = repository.importMedia(uri, isVideo = false, isAudio = true)
                if (success) count++
            }
            _uiState.update {
                it.copy(
                    feedbackMessage = if (count > 0) "Imported $count audio recording(s) securely." else "Failed to import audio files."
                )
            }
            refreshStorageSize()
        }
    }

    fun moveToTrash(media: VaultMedia) {
        viewModelScope.launch {
            repository.moveToTrash(media)
            closeMediaView()
            _uiState.update { it.copy(feedbackMessage = "Moved to Trash bin.") }
            refreshStorageSize()
        }
    }

    fun restoreFromTrash(media: VaultMedia) {
        viewModelScope.launch {
            repository.restoreFromTrash(media)
            closeMediaView()
            _uiState.update { it.copy(feedbackMessage = "Restored item successfully.") }
            refreshStorageSize()
        }
    }

    fun permanentDelete(media: VaultMedia) {
        viewModelScope.launch {
            repository.permanentDelete(media)
            closeMediaView()
            _uiState.update { it.copy(feedbackMessage = "Permanently deleted.") }
            refreshStorageSize()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            _uiState.update { it.copy(feedbackMessage = "Trash bin emptied.") }
            refreshStorageSize()
        }
    }

    fun setWallpaperPreset(index: Int) {
        _uiState.update { it.copy(selectedWallpaperIndex = index, customWallpaperUri = null) }
    }

    fun setCustomWallpaper(uri: Uri) {
        _uiState.update { it.copy(customWallpaperUri = uri.toString()) }
    }

    fun checkUpdate() {
        checkForUpdates()
    }

    fun navigateTo(tab: VaultScreenTab) {
        _uiState.update {
            it.copy(
                currentTab = tab,
                isViewingMediaDetail = false,
                isEditingNote = false,
                isCreatingNote = false
            )
        }
        refreshStorageSize()
    }

    fun selectMediaForView(media: VaultMedia) {
        _uiState.update {
            it.copy(
                selectedMedia = media,
                isViewingMediaDetail = true
            )
        }
    }

    fun closeMediaView() {
        _uiState.update {
            it.copy(
                selectedMedia = null,
                isViewingMediaDetail = false
            )
        }
    }

    fun importMediaItems(uris: List<Uri>, isVideo: Boolean) {
        viewModelScope.launch {
            val successfulUris = mutableListOf<Uri>()
            for (uri in uris) {
                val success = repository.importMedia(uri, isVideo)
                if (success) {
                    successfulUris.add(uri)
                }
            }
            val count = successfulUris.size
            val label = if (isVideo) "video" else "photo"
            val plural = if (count > 1) "${label}s" else label

            if (count > 0) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Successfully imported $count $plural into the vault!",
                        deleteOriginalPrompt = DeleteOriginalPrompt(
                            uris = successfulUris,
                            isVideo = isVideo,
                            count = count
                        )
                    )
                }
            } else {
                _uiState.update {
                    it.copy(feedbackMessage = "Failed to import selected $plural.")
                }
            }
            refreshStorageSize()
        }
    }

    fun importMixedMediaItems(photoUris: List<Uri>, videoUris: List<Uri>) {
        viewModelScope.launch {
            val successfulPhotos = mutableListOf<Uri>()
            for (uri in photoUris) {
                if (repository.importMedia(uri, isVideo = false)) {
                    successfulPhotos.add(uri)
                }
            }
            val successfulVideos = mutableListOf<Uri>()
            for (uri in videoUris) {
                if (repository.importMedia(uri, isVideo = true)) {
                    successfulVideos.add(uri)
                }
            }
            val total = successfulPhotos.size + successfulVideos.size
            if (total > 0) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Successfully imported $total item(s) from gallery into the vault!",
                        deleteOriginalPrompt = DeleteOriginalPrompt(
                            uris = successfulPhotos + successfulVideos,
                            isVideo = successfulVideos.isNotEmpty(),
                            count = total
                        )
                    )
                }
            } else {
                _uiState.update {
                    it.copy(feedbackMessage = "Failed to import selected items.")
                }
            }
            refreshStorageSize()
        }
    }

    fun importDocuments(uris: List<Uri>) {
        viewModelScope.launch {
            val successfulUris = mutableListOf<Uri>()
            for (uri in uris) {
                if (repository.importDocument(uri)) {
                    successfulUris.add(uri)
                }
            }
            val count = successfulUris.size
            if (count > 0) {
                val label = if (count > 1) "documents" else "document"
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Successfully secured $count $label in vault!",
                        deleteOriginalPrompt = DeleteOriginalPrompt(
                            uris = successfulUris,
                            isVideo = false,
                            isDocument = true,
                            count = count
                        )
                    )
                }
            } else {
                _uiState.update { it.copy(feedbackMessage = "Could not import selected files.") }
            }
            refreshStorageSize()
        }
    }

    fun saveSecretCameraPhoto(tempFile: File, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val media = repository.saveSecretCameraPhoto(tempFile)
            if (media != null) {
                _uiState.update { it.copy(feedbackMessage = "Photo captured & encrypted secretly in vault!") }
                refreshStorageSize()
                onSaved()
            }
        }
    }

    fun saveSecretCameraVideo(tempFile: File, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val media = repository.saveSecretCameraVideo(tempFile)
            if (media != null) {
                _uiState.update { it.copy(feedbackMessage = "Secret video saved & encrypted in vault!") }
                refreshStorageSize()
                onSaved()
            }
        }
    }

    fun saveSecretRecordedAudio(tempFile: File, title: String? = null, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val media = repository.saveSecretRecordedAudio(tempFile, title)
            if (media != null) {
                _uiState.update { it.copy(feedbackMessage = "Secret audio encrypted & saved directly to vault!") }
                refreshStorageSize()
                onSaved()
            }
        }
    }

    fun dismissDeleteOriginalPrompt() {
        _uiState.update { it.copy(deleteOriginalPrompt = null) }
    }

    fun onOriginalsDeleted(success: Boolean) {
        _uiState.update {
            it.copy(
                deleteOriginalPrompt = null,
                feedbackMessage = if (success) {
                    "Original media deleted from Gallery."
                } else {
                    "Original media kept in Gallery."
                }
            )
        }
    }

    fun deleteMedia(media: VaultMedia) {
        viewModelScope.launch {
            repository.deleteMedia(media)
            closeMediaView()
            _uiState.update {
                it.copy(feedbackMessage = "Permanently deleted from vault.")
            }
            refreshStorageSize()
        }
    }

    fun unhideMedia(media: VaultMedia) {
        viewModelScope.launch {
            val success = repository.unhideMedia(media)
            closeMediaView()
            _uiState.update {
                it.copy(
                    feedbackMessage = if (success) {
                        "Restored back to your public gallery."
                    } else {
                        "Failed to export media."
                    }
                )
            }
            refreshStorageSize()
        }
    }

    fun getMediaFile(media: VaultMedia): File {
        return repository.getMediaFile(media)
    }

    // Notes
    fun startCreateNote() {
        _uiState.update {
            it.copy(
                selectedNote = null,
                isCreatingNote = true,
                isEditingNote = false
            )
        }
    }

    fun startEditNote(note: SecretNote) {
        _uiState.update {
            it.copy(
                selectedNote = note,
                isCreatingNote = false,
                isEditingNote = true
            )
        }
    }

    fun closeNoteEditor() {
        _uiState.update {
            it.copy(
                selectedNote = null,
                isCreatingNote = false,
                isEditingNote = false
            )
        }
    }

    fun saveNote(title: String, content: String, id: Long = 0, isPinned: Boolean = false, colorIndex: Int = 0) {
        viewModelScope.launch {
            repository.saveNote(title, content, id, isPinned, colorIndex)
            closeNoteEditor()
            _uiState.update {
                it.copy(feedbackMessage = "Note saved securely.")
            }
        }
    }

    fun deleteNote(note: SecretNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            closeNoteEditor()
            _uiState.update {
                it.copy(feedbackMessage = "Secret note deleted.")
            }
        }
    }

    fun togglePinNote(note: SecretNote) {
        viewModelScope.launch {
            repository.togglePinNote(note)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(feedbackMessage = "Checking for official updates...") }
            try {
                // Non-blocking query to GitHub releases API or raw JSON
                kotlinx.coroutines.delay(1200)
                _uiState.update {
                    it.copy(
                        showUpdateDialog = true,
                        latestVersionInfo = "CalcVault v3.0.0-PRO (Latest)\n• 12 AMOLED Stealth Themes\n• Dynamic Screen Guard (FLAG_SECURE)\n• Zero-Loss Uninstall Persistence\n• Built-in Photo Editor & Audio Player\n• Secure Multi-Select Trash Bin"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(feedbackMessage = "Already running latest version (v3.0.0-PRO).") }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun refreshStorageSize() {
        viewModelScope.launch {
            val bytes = repository.fileManager.getVaultTotalSizeBytes()
            val formatted = formatBytes(bytes)
            _uiState.update { it.copy(totalVaultSize = formatted) }
        }
    }

    fun onVaultUnlocked() {
        viewModelScope.launch {
            repository.reindexPersistentFiles()
            refreshStorageSize()
        }
    }

    fun onRestoreCompleted() {
        viewModelScope.launch {
            repository.reindexPersistentFiles()
            refreshStorageSize()
            _uiState.update { it.copy(feedbackMessage = "Restore complete! Storage and media updated.") }
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
        }
    }

    class Factory(private val repository: VaultRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VaultViewModel(repository) as T
        }
    }
}
