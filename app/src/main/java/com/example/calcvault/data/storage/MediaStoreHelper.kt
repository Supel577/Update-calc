package com.example.calcvault.data.storage

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.result.IntentSenderRequest

object MediaStoreHelper {

    /**
     * Resolves an arbitrary media content Uri (such as from Photo Picker or SAF)
     * to a MediaStore content Uri that can be used with MediaStore.createDeleteRequest.
     */
    fun resolveToMediaStoreUri(context: Context, uri: Uri, isVideo: Boolean): Uri {
        val uriStr = uri.toString()
        val baseUri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // 1. Already a standard MediaStore external URI
        if (uriStr.startsWith("content://media/external/images/media/") ||
            uriStr.startsWith("content://media/external/video/media/")
        ) {
            return uri
        }

        // 2. Extract numeric ID from last path segment (e.g. Photo Picker: content://media/picker/.../12345)
        val id = uri.lastPathSegment?.toLongOrNull()
        if (id != null) {
            val candidate = ContentUris.withAppendedId(baseUri, id)
            try {
                context.contentResolver.query(
                    candidate,
                    arrayOf(MediaStore.MediaColumns._ID),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return candidate
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: match by DISPLAY_NAME and SIZE in MediaStore
        try {
            var displayName: String? = null
            var size: Long = -1L
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) displayName = c.getString(nameIdx)
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0) size = c.getLong(sizeIdx)
                }
            }

            if (!displayName.isNullOrBlank()) {
                val selection = if (size > 0) {
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?"
                } else {
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                }
                val args = if (size > 0) arrayOf(displayName, size.toString()) else arrayOf(displayName)
                context.contentResolver.query(
                    baseUri,
                    arrayOf(MediaStore.MediaColumns._ID),
                    selection,
                    args,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val matchedId = cursor.getLong(0)
                        return ContentUris.withAppendedId(baseUri, matchedId)
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. If numeric ID was present, use candidate even if query couldn't verify
        if (id != null) {
            return ContentUris.withAppendedId(baseUri, id)
        }

        return uri
    }

    /**
     * Triggers the modern MediaStore deletion request on Android 11+ (API 30+)
     * or ContentResolver delete on older Android versions.
     */
    fun requestDelete(
        context: Context,
        uris: List<Uri>,
        isVideo: Boolean,
        onLaunchIntentSender: (IntentSenderRequest) -> Unit,
        onDirectResult: (Boolean) -> Unit
    ) {
        if (uris.isEmpty()) {
            onDirectResult(true)
            return
        }

        val resolvedUris = uris.map { resolveToMediaStoreUri(context, it, isVideo) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) MediaStore.createDeleteRequest
            try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, resolvedUris)
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                onLaunchIntentSender(intentSenderRequest)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to direct contentResolver delete
                var deletedCount = 0
                for (u in resolvedUris) {
                    try {
                        val count = context.contentResolver.delete(u, null, null)
                        if (count > 0) deletedCount++
                    } catch (_: Exception) {}
                }
                onDirectResult(deletedCount > 0)
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Android 10 (API 29) RecoverableSecurityException handling
            try {
                var deletedCount = 0
                for (u in resolvedUris) {
                    try {
                        val count = context.contentResolver.delete(u, null, null)
                        if (count > 0) deletedCount++
                    } catch (secEx: RecoverableSecurityException) {
                        onLaunchIntentSender(
                            IntentSenderRequest.Builder(secEx.userAction.actionIntent.intentSender).build()
                        )
                        return
                    }
                }
                onDirectResult(deletedCount > 0)
            } catch (e: Exception) {
                e.printStackTrace()
                onDirectResult(false)
            }
        } else {
            // Android 9 and older
            var deletedCount = 0
            for (u in resolvedUris) {
                try {
                    val count = context.contentResolver.delete(u, null, null)
                    if (count > 0) deletedCount++
                } catch (_: Exception) {}
            }
            onDirectResult(deletedCount > 0)
        }
    }
}
