package com.example.calcvault.data.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.calcvault.data.model.VaultMedia
import com.example.calcvault.data.model.VaultMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class VaultMetaInfo(
    val pinHash: String,
    val pinLength: Int,
    val securityQuestion: String?,
    val securityAnswerHash: String?,
    val timestamp: Long
)

class VaultFileManager(private val context: Context) {

    val vaultMediaDir: File by lazy {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val chosenDir = File(baseDir, ".calcvault_secure_data")

        try {
            if (!chosenDir.exists()) {
                chosenDir.mkdirs()
            }
            val nomedia = File(chosenDir, ".nomedia")
            if (!nomedia.exists()) {
                nomedia.createNewFile()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        chosenDir
    }

    val photosDir: File by lazy {
        File(vaultMediaDir, "photos").apply { try { if (!exists()) mkdirs() } catch (_: Throwable) {} }
    }

    val videosDir: File by lazy {
        File(vaultMediaDir, "videos").apply { try { if (!exists()) mkdirs() } catch (_: Throwable) {} }
    }

    val audiosDir: File by lazy {
        File(vaultMediaDir, "audios").apply { try { if (!exists()) mkdirs() } catch (_: Throwable) {} }
    }

    val documentsDir: File by lazy {
        File(vaultMediaDir, "documents").apply { try { if (!exists()) mkdirs() } catch (_: Throwable) {} }
    }

    fun saveVaultMeta(pinHash: String, pinLength: Int, securityQuestion: String?, securityAnswerHash: String?) {
        try {
            val metaFile = File(vaultMediaDir, "vault_meta.dat")
            val json = org.json.JSONObject().apply {
                put("pinHash", pinHash)
                put("pinLength", pinLength)
                put("securityQuestion", securityQuestion ?: "")
                put("securityAnswerHash", securityAnswerHash ?: "")
                put("timestamp", System.currentTimeMillis())
            }
            metaFile.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readVaultMeta(): VaultMetaInfo? {
        val candidateFiles = listOfNotNull(
            try { File(vaultMediaDir, "vault_meta.dat") } catch (_: Throwable) { null },
            try { context.getExternalFilesDir(null)?.let { File(File(it, ".calcvault_secure_data"), "vault_meta.dat") } } catch (_: Throwable) { null },
            try { File(context.filesDir, "vault_meta.dat") } catch (_: Throwable) { null }
        )
        for (file in candidateFiles) {
            if (file.exists()) {
                try {
                    val json = org.json.JSONObject(file.readText())
                    val pinHash = json.optString("pinHash", "")
                    if (pinHash.isNotEmpty()) {
                        return VaultMetaInfo(
                            pinHash = pinHash,
                            pinLength = json.optInt("pinLength", 4),
                            securityQuestion = json.optString("securityQuestion").ifEmpty { null },
                            securityAnswerHash = json.optString("securityAnswerHash").ifEmpty { null },
                            timestamp = json.optLong("timestamp", 0L)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    fun saveNotesBackup(notesJson: String) {
        try {
            File(vaultMediaDir, "notes_backup.json").writeText(notesJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readNotesBackup(): String? {
        val candidateFiles = listOfNotNull(
            File(vaultMediaDir, "notes_backup.json"),
            try { File(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), ".calcvault_secure_data"), "notes_backup.json") } catch (_: Exception) { null }
        )
        for (f in candidateFiles) {
            if (f.exists()) {
                try {
                    return f.readText()
                } catch (_: Exception) {}
            }
        }
        return null
    }

    fun hasExistingVaultData(): Boolean {
        if (readVaultMeta() != null) return true
        val discovered = getDiscoveredFiles()
        return discovered.isNotEmpty()
    }

    suspend fun saveSecretCameraPhoto(tempFile: File): VaultMedia? = withContext(Dispatchers.IO) {
        try {
            val uniqueFileName = "vault_cam_${UUID.randomUUID()}.jpg"
            val destFile = File(photosDir, uniqueFileName)
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
            VaultMedia(
                fileName = uniqueFileName,
                originalName = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg",
                mediaType = VaultMediaType.PHOTO,
                sizeBytes = destFile.length(),
                dateAdded = System.currentTimeMillis(),
                relativePath = "photos/$uniqueFileName"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveSecretCameraVideo(tempFile: File): VaultMedia? = withContext(Dispatchers.IO) {
        try {
            val uniqueFileName = "vault_cam_${UUID.randomUUID()}.mp4"
            val destFile = File(videosDir, uniqueFileName)
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
            VaultMedia(
                fileName = uniqueFileName,
                originalName = "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4",
                mediaType = VaultMediaType.VIDEO,
                sizeBytes = destFile.length(),
                dateAdded = System.currentTimeMillis(),
                relativePath = "videos/$uniqueFileName"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveSecretRecordedAudio(tempFile: File, customTitle: String? = null): VaultMedia? = withContext(Dispatchers.IO) {
        try {
            if (!tempFile.exists() || tempFile.length() <= 0L) {
                return@withContext null
            }
            val uniqueFileName = "vault_rec_${UUID.randomUUID()}.m4a"
            val destFile = File(audiosDir, uniqueFileName)
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()

            val nowFormatted = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val originalName = if (!customTitle.isNullOrBlank()) {
                val clean = customTitle.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
                if (clean.endsWith(".m4a", ignoreCase = true) || clean.endsWith(".mp3", ignoreCase = true)) clean else "$clean.m4a"
            } else {
                "REC_$nowFormatted.m4a"
            }

            VaultMedia(
                fileName = uniqueFileName,
                originalName = originalName,
                mediaType = VaultMediaType.AUDIO,
                sizeBytes = destFile.length(),
                dateAdded = System.currentTimeMillis(),
                relativePath = "audios/$uniqueFileName"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importDocument(uri: Uri): VaultMedia? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val originalName = getFileNameFromUri(uri) ?: "doc_${System.currentTimeMillis()}.bin"
            val ext = originalName.substringAfterLast('.', "bin")
            val uniqueFileName = "vault_doc_${UUID.randomUUID()}.$ext"
            val destinationFile = File(documentsDir, uniqueFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val fileSize = destinationFile.length()
            VaultMedia(
                fileName = uniqueFileName,
                originalName = originalName,
                mediaType = VaultMediaType.DOCUMENT,
                sizeBytes = fileSize,
                dateAdded = System.currentTimeMillis(),
                relativePath = "documents/$uniqueFileName"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importMedia(uri: Uri, isVideo: Boolean, isAudio: Boolean = false): VaultMedia? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val defaultExt = when {
                isAudio -> "mp3"
                isVideo -> "mp4"
                else -> "jpg"
            }
            val originalName = getFileNameFromUri(uri) ?: "media_${System.currentTimeMillis()}.$defaultExt"
            val targetDir = when {
                isAudio -> audiosDir
                isVideo -> videosDir
                else -> photosDir
            }
            val mediaType = when {
                isAudio -> VaultMediaType.AUDIO
                isVideo -> VaultMediaType.VIDEO
                else -> VaultMediaType.PHOTO
            }
            val ext = originalName.substringAfterLast('.', defaultExt)
            val uniqueFileName = "vault_${UUID.randomUUID()}.$ext"
            val destinationFile = File(targetDir, uniqueFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val fileSize = destinationFile.length()
            val subFolder = when {
                isAudio -> "audios"
                isVideo -> "videos"
                else -> "photos"
            }

            VaultMedia(
                fileName = uniqueFileName,
                originalName = originalName,
                mediaType = mediaType,
                sizeBytes = fileSize,
                dateAdded = System.currentTimeMillis(),
                relativePath = "$subFolder/$uniqueFileName"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveBitmapToVault(bitmap: Bitmap, title: String): VaultMedia? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").trim()
            val uniqueFileName = "vault_screenshot_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
            val destinationFile = File(photosDir, uniqueFileName)
            FileOutputStream(destinationFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val fileSize = destinationFile.length()
            VaultMedia(
                fileName = uniqueFileName,
                originalName = if (cleanTitle.isNotBlank()) "$cleanTitle.png" else "Screenshot.png",
                mediaType = VaultMediaType.PHOTO,
                sizeBytes = fileSize,
                dateAdded = System.currentTimeMillis(),
                relativePath = "photos/$uniqueFileName"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadMediaDirectly(
        url: String,
        contentDisposition: String?,
        mimeType: String?
    ): VaultMedia? = withContext(Dispatchers.IO) {
        try {
            // Handle Data URIs (e.g. data:image/jpeg;base64,...) common on Google Images
            if (url.startsWith("data:", ignoreCase = true)) {
                val isImage = url.startsWith("data:image", ignoreCase = true)
                val ext = when {
                    url.contains("image/png", ignoreCase = true) -> "png"
                    url.contains("image/webp", ignoreCase = true) -> "webp"
                    url.contains("image/gif", ignoreCase = true) -> "gif"
                    isImage -> "jpg"
                    else -> "bin"
                }
                val base64Data = if (url.contains("base64,")) url.substringAfter("base64,") else ""
                if (base64Data.isNotBlank()) {
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val uniqueFileName = "vault_dl_${UUID.randomUUID()}.$ext"
                    val destinationFile = File(if (isImage) photosDir else documentsDir, uniqueFileName)
                    FileOutputStream(destinationFile).use { it.write(bytes) }
                    return@withContext VaultMedia(
                        fileName = uniqueFileName,
                        originalName = "Downloaded_Image.$ext",
                        mediaType = if (isImage) VaultMediaType.PHOTO else VaultMediaType.DOCUMENT,
                        sizeBytes = destinationFile.length(),
                        dateAdded = System.currentTimeMillis(),
                        relativePath = if (isImage) "photos/$uniqueFileName" else "documents/$uniqueFileName"
                    )
                }
            }

            val guessedName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
            val isVideo = (mimeType != null && mimeType.startsWith("video")) ||
                    guessedName.endsWith(".mp4", ignoreCase = true) ||
                    guessedName.endsWith(".mkv", ignoreCase = true) ||
                    guessedName.endsWith(".webm", ignoreCase = true) ||
                    guessedName.endsWith(".mov", ignoreCase = true)

            val targetDir = if (isVideo) videosDir else photosDir
            val ext = guessedName.substringAfterLast('.', if (isVideo) "mp4" else "jpg")
            val uniqueFileName = "vault_dl_${UUID.randomUUID()}.$ext"
            val destinationFile = File(targetDir, uniqueFileName)

            val client = okhttp3.OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                FileOutputStream(destinationFile).use { out ->
                    body.byteStream().copyTo(out)
                }
            }

            val fileSize = destinationFile.length()
            VaultMedia(
                fileName = uniqueFileName,
                originalName = guessedName,
                mediaType = if (isVideo) VaultMediaType.VIDEO else VaultMediaType.PHOTO,
                sizeBytes = fileSize,
                dateAdded = System.currentTimeMillis(),
                relativePath = if (isVideo) "videos/$uniqueFileName" else "photos/$uniqueFileName"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFile(media: VaultMedia): File {
        val targetDir = when (media.mediaType) {
            VaultMediaType.VIDEO -> videosDir
            VaultMediaType.AUDIO -> audiosDir
            VaultMediaType.DOCUMENT -> documentsDir
            else -> photosDir
        }
        val primaryFile = File(targetDir, media.fileName)
        if (primaryFile.exists()) return primaryFile

        // Check relativePath inside vaultMediaDir
        if (media.relativePath.isNotBlank()) {
            val relFile = File(vaultMediaDir, media.relativePath)
            if (relFile.exists()) return relFile
        }

        // Check directly inside vaultMediaDir root
        val directFile = File(vaultMediaDir, media.fileName)
        if (directFile.exists()) return directFile

        // Check all potential fallback directories across app storage & public documents
        val altRoots = listOfNotNull(
            try { File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), ".calcvault_secure_data") } catch (_: Exception) { null },
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { File(it, ".calcvault_secure_data") },
            context.getExternalFilesDir(null)?.let { File(it, ".calcvault_secure_data") },
            File(context.filesDir, ".calcvault_secure_data")
        )
        for (alt in altRoots) {
            val subDir = when (media.mediaType) {
                VaultMediaType.VIDEO -> File(alt, "videos")
                VaultMediaType.AUDIO -> File(alt, "audios")
                VaultMediaType.DOCUMENT -> File(alt, "documents")
                else -> File(alt, "photos")
            }
            val subFile = File(subDir, media.fileName)
            if (subFile.exists()) return subFile

            if (media.relativePath.isNotBlank()) {
                val relAlt = File(alt, media.relativePath)
                if (relAlt.exists()) return relAlt
            }

            val directAlt = File(alt, media.fileName)
            if (directAlt.exists()) return directAlt
        }

        return primaryFile
    }

    suspend fun deleteMediaFile(media: VaultMedia): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getFile(media)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportBackToPublicGallery(media: VaultMedia): Boolean = withContext(Dispatchers.IO) {
        val sourceFile = getFile(media)
        if (!sourceFile.exists()) return@withContext false

        try {
            val resolver = context.contentResolver
            val isVideo = media.mediaType == VaultMediaType.VIDEO
            val isAudio = media.mediaType == VaultMediaType.AUDIO
            val isDoc = media.mediaType == VaultMediaType.DOCUMENT
            val mimeType = getMimeType(media.originalName, isVideo, isAudio, isDoc)
            val subFolder = "Restored"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = when {
                    isVideo -> Environment.DIRECTORY_MOVIES + File.separator + subFolder
                    isAudio -> Environment.DIRECTORY_MUSIC + File.separator + subFolder
                    isDoc -> Environment.DIRECTORY_DOWNLOADS + File.separator + subFolder
                    else -> Environment.DIRECTORY_PICTURES + File.separator + subFolder
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, media.originalName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = when {
                    isVideo -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    isAudio -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    isDoc -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val itemUri = resolver.insert(collectionUri, contentValues) ?: return@withContext false

                resolver.openOutputStream(itemUri)?.use { output ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(output)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)

                // Rescan MediaStore so gallery and file managers update immediately
                try {
                    resolver.query(itemUri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val path = cursor.getString(0)
                            if (!path.isNullOrBlank()) {
                                MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType), null)
                            }
                        }
                    }
                } catch (_: Exception) {}

                true
            } else {
                // Pre-Android 10 (API < 29)
                val targetDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        when {
                            isVideo -> Environment.DIRECTORY_MOVIES
                            isAudio -> Environment.DIRECTORY_MUSIC
                            isDoc -> Environment.DIRECTORY_DOWNLOADS
                            else -> Environment.DIRECTORY_PICTURES
                        }
                    ),
                    subFolder
                )
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val destFile = File(targetDir, media.originalName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getMimeType(fileName: String, isVideo: Boolean, isAudio: Boolean = false, isDoc: Boolean = false): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isNotEmpty()) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) return mime
        }
        return when {
            isVideo -> "video/mp4"
            isAudio -> "audio/mpeg"
            isDoc -> "application/octet-stream"
            else -> "image/jpeg"
        }
    }

    fun getVaultTotalSizeBytes(): Long {
        return calculateDirectorySize(vaultMediaDir)
    }

    fun getDiscoveredFiles(): List<VaultMedia> {
        val discovered = mutableListOf<VaultMedia>()
        val seenFileNames = mutableSetOf<String>()

        fun scanDir(dir: File, type: VaultMediaType, subFolder: String) {
            if (!dir.exists()) return
            dir.listFiles()?.forEach { f ->
                if (f.isFile && f.name != ".nomedia" && !f.name.startsWith(".") && seenFileNames.add(f.name)) {
                    discovered.add(
                        VaultMedia(
                            fileName = f.name,
                            originalName = f.name.removePrefix("vault_").removePrefix("vault_dl_").removePrefix("vault_doc_").removePrefix("vault_cam_"),
                            mediaType = type,
                            sizeBytes = f.length(),
                            dateAdded = f.lastModified(),
                            relativePath = "$subFolder/${f.name}"
                        )
                    )
                }
            }
        }

        // Scan primary active directories
        scanDir(photosDir, VaultMediaType.PHOTO, "photos")
        scanDir(videosDir, VaultMediaType.VIDEO, "videos")
        scanDir(audiosDir, VaultMediaType.AUDIO, "audios")
        scanDir(documentsDir, VaultMediaType.DOCUMENT, "documents")

        fun scanRoot(dir: File) {
            if (!dir.exists()) return
            dir.listFiles()?.forEach { f ->
                if (f.isFile && f.name != ".nomedia" && !f.name.startsWith(".") && seenFileNames.add(f.name)) {
                    val type = when {
                        f.name.endsWith(".mp4", true) || f.name.endsWith(".mkv", true) || f.name.endsWith(".mov", true) || f.name.endsWith(".3gp", true) -> VaultMediaType.VIDEO
                        f.name.endsWith(".mp3", true) || f.name.endsWith(".wav", true) || f.name.endsWith(".m4a", true) || f.name.endsWith(".aac", true) -> VaultMediaType.AUDIO
                        f.name.endsWith(".pdf", true) || f.name.endsWith(".doc", true) || f.name.endsWith(".docx", true) || f.name.endsWith(".txt", true) || f.name.endsWith(".zip", true) -> VaultMediaType.DOCUMENT
                        else -> VaultMediaType.PHOTO
                    }
                    discovered.add(
                        VaultMedia(
                            fileName = f.name,
                            originalName = f.name.removePrefix("vault_").removePrefix("vault_dl_").removePrefix("vault_doc_").removePrefix("vault_cam_"),
                            mediaType = type,
                            sizeBytes = f.length(),
                            dateAdded = f.lastModified(),
                            relativePath = f.name
                        )
                    )
                }
            }
        }
        scanRoot(vaultMediaDir)

        // Also search alternate locations where previous installs could have kept data
        val altRoots = listOfNotNull(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { File(it, ".calcvault_secure_data") },
            context.getExternalFilesDir(null)?.let { File(it, ".calcvault_secure_data") },
            File(context.filesDir, ".calcvault_secure_data"),
            try { File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), ".calcvault_secure_data") } catch (_: Exception) { null }
        )
        for (alt in altRoots) {
            if (alt.exists() && alt.absolutePath != vaultMediaDir.absolutePath) {
                scanDir(File(alt, "photos"), VaultMediaType.PHOTO, "photos")
                scanDir(File(alt, "videos"), VaultMediaType.VIDEO, "videos")
                scanDir(File(alt, "audios"), VaultMediaType.AUDIO, "audios")
                scanDir(File(alt, "documents"), VaultMediaType.DOCUMENT, "documents")
                scanRoot(alt)
            }
        }

        return discovered
    }

    private fun calculateDirectorySize(dir: File): Long {
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
        }
        return size
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    fun deleteAllVaultFiles(): Boolean {
        return try {
            val roots = listOfNotNull(
                vaultMediaDir,
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { File(it, ".calcvault_secure_data") },
                context.getExternalFilesDir(null)?.let { File(it, ".calcvault_secure_data") },
                File(context.filesDir, ".calcvault_secure_data"),
                try { File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), ".calcvault_secure_data") } catch (_: Exception) { null }
            )
            for (root in roots) {
                if (root.exists()) {
                    root.listFiles()?.forEach { child ->
                        child.deleteRecursively()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
