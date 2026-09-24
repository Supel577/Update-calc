package com.example.calcvault.data.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleDriveBackupService(private val context: Context) {

    private val prefs = context.getSharedPreferences("calc_vault_cloud_prefs", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val driveScope = "https://www.googleapis.com/auth/drive.appdata"

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(driveScope))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(driveScope))) {
            return account
        }
        return null
    }

    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                prefs.edit().putString(KEY_LAST_ACCOUNT, account.email).apply()
                Result.success(account)
            } else {
                Result.failure(Exception("Google Sign-In returned null account."))
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveBackup", "Sign in failed: ${e.message}", e)
            val friendlyMsg = if (e is ApiException) {
                when (e.statusCode) {
                    10 -> "Google Cloud Client ID / SHA-1 fingerprint needs registration for in-app API. Please use 'Save directly to Google Drive' below for instant 1-tap backup!"
                    12500 -> "Google Sign-In failed (Code 12500). Please check internet or use 'Save directly to Google Drive' below."
                    16 -> "Sign-in was cancelled."
                    7 -> "Network connection error. Please verify your internet connection."
                    else -> "Google error (${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}. Please use 'Save directly to Google Drive' below."
                }
            } else {
                e.localizedMessage ?: "Google Sign-in failed. Please use 'Save directly to Google Drive' below."
            }
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        googleSignInClient.signOut().addOnCompleteListener {
            prefs.edit().remove(KEY_LAST_ACCOUNT).apply()
            onComplete()
        }
    }

    suspend fun getAccessToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val email = account.email ?: account.account?.name ?: return@withContext null
            GoogleAuthUtil.getToken(context, email, "oauth2:$driveScope")
        } catch (e: Exception) {
            Log.e("GoogleDriveBackup", "Failed to get access token: ${e.message}", e)
            null
        }
    }

    suspend fun uploadBackupToAppData(
        zipFile: File,
        onProgress: (String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
                ?: return@withContext Result.failure(Exception("Please sign in to Google Drive first."))

            onProgress("Authorizing with Google Drive...")
            val token = getAccessToken(account)
                ?: return@withContext Result.failure(Exception("Unable to acquire Google Drive authorization token."))

            // Check if backup already exists in appDataFolder to clean up old copies
            onProgress("Querying cloud AppData folder...")
            val existingFileId = findExistingBackupFileId(token)

            if (existingFileId != null) {
                onProgress("Updating existing cloud backup...")
                // Delete old backup to maintain single latest state
                deleteFile(token, existingFileId)
            }

            onProgress("Uploading encrypted backup (${zipFile.length() / 1024} KB)...")
            val metadataPart = JSONObject().apply {
                put("name", "calcvault_backup.zip")
                put("parents", org.json.JSONArray().put("appDataFolder"))
            }.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())

            val filePart = zipFile.asRequestBody("application/zip".toMediaType())

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", "metadata", metadataPart)
                .addFormDataPart("file", zipFile.name, filePart)
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .header("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: response.message
                    return@withContext Result.failure(Exception("Upload failed: $err"))
                }

                val resJson = JSONObject(response.body?.string() ?: "{}")
                val fileId = resJson.optString("id", "")

                // Record success
                val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                val timeStr = dateFormat.format(Date())
                val sizeStr = "${"%.1f".format(zipFile.length() / (1024.0 * 1024.0))} MB"

                prefs.edit()
                    .putString(KEY_LAST_BACKUP_TIME, timeStr)
                    .putString(KEY_LAST_BACKUP_SIZE, sizeStr)
                    .apply()

                onProgress("Cloud backup completed successfully!")
                Result.success(fileId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun downloadBackupFromAppData(
        destZip: File,
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount()
                ?: return@withContext Result.failure(Exception("Please sign in to Google Drive first."))

            onProgress("Authorizing with Google Drive...")
            val token = getAccessToken(account)
                ?: return@withContext Result.failure(Exception("Unable to acquire Google Drive authorization token."))

            onProgress("Searching for backup in AppData folder...")
            val fileId = findExistingBackupFileId(token)
                ?: return@withContext Result.failure(Exception("No backup found in Google Drive AppData."))

            onProgress("Downloading backup file...")
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Download failed: HTTP ${response.code}"))
                }

                destZip.parentFile?.mkdirs()
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(destZip).use { output ->
                        input.copyTo(output)
                    }
                }

                onProgress("Download completed (${destZip.length() / 1024} KB)")
                Result.success(destZip)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun findExistingBackupFileId(token: String): String? {
        val queryUrl = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='calcvault_backup.zip' and trashed=false&fields=files(id,name,size)"
        val request = Request.Builder()
            .url(queryUrl)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    val json = JSONObject(body)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        files.getJSONObject(0).optString("id")
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun deleteFile(token: String, fileId: String) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        try {
            httpClient.newCall(request).execute().close()
        } catch (_: Exception) {}
    }

    fun getLastBackupInfo(): Pair<String?, String?> {
        val time = prefs.getString(KEY_LAST_BACKUP_TIME, null)
        val size = prefs.getString(KEY_LAST_BACKUP_SIZE, null)
        return Pair(time, size)
    }

    companion object {
        private const val KEY_LAST_ACCOUNT = "key_last_google_account"
        private const val KEY_LAST_BACKUP_TIME = "key_last_backup_time"
        private const val KEY_LAST_BACKUP_SIZE = "key_last_backup_size"
    }
}
