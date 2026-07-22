package dev.gaborbiro.dailymacros.repositories.backup

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.backup.di.ForDrive
import dev.gaborbiro.dailymacros.repositories.backup.domain.CloudSyncRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DriveBackupInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ForDrive private val okHttpClient: OkHttpClient,
) : CloudSyncRepository {

    private val gson = Gson()

    override suspend fun getBackupInfo(accessToken: String): DriveBackupInfo? =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$FILES_URL?spaces=appDataFolder&fields=files(id,name,modifiedTime,size)&q=name='$BACKUP_FILE_NAME'")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw driveApiException(response.code, response.body?.string())
                val body = response.body?.string() ?: return@use null
                val list = gson.fromJson(body, DriveFileListResponse::class.java)
                list.files.firstOrNull()?.toDriveBackupInfo()
            }
        }

    override suspend fun uploadBackup(accessToken: String, tarFile: File): DriveBackupInfo =
        withContext(Dispatchers.IO) {
            val existing = getBackupInfo(accessToken)
            if (existing != null) {
                updateFile(accessToken, existing.fileId, tarFile)
            } else {
                createFile(accessToken, tarFile)
            }
        }

    override suspend fun downloadBackupToTempFile(accessToken: String, fileId: String): File =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$FILES_URL/$fileId?alt=media")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()
            val tempFile = File(appContext.cacheDir, "drive_download_${System.nanoTime()}.tar")
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw driveApiException(response.code, response.body?.string())
                val body = response.body ?: throw IOException("Empty response body from Drive download")
                tempFile.outputStream().use { out -> body.byteStream().copyTo(out) }
            }
            tempFile
        }

    private fun createFile(accessToken: String, tarFile: File): DriveBackupInfo {
        val metadata = """{"name":"$BACKUP_FILE_NAME","parents":["appDataFolder"]}"""
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(tarFile.asRequestBody("application/octet-stream".toMediaType()))
            .build()
        val request = Request.Builder()
            .url("$UPLOAD_URL?uploadType=multipart&fields=id,modifiedTime,size")
            .header("Authorization", "Bearer $accessToken")
            .post(body)
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw driveApiException(response.code, response.body?.string())
            val responseBody = response.body?.string() ?: throw IOException("Empty response from Drive upload")
            gson.fromJson(responseBody, DriveFileResponse::class.java).toDriveBackupInfo()
        }
    }

    private fun updateFile(accessToken: String, fileId: String, tarFile: File): DriveBackupInfo {
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart("{}".toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(tarFile.asRequestBody("application/octet-stream".toMediaType()))
            .build()
        val request = Request.Builder()
            .url("$UPLOAD_URL/$fileId?uploadType=multipart&fields=id,modifiedTime,size")
            .header("Authorization", "Bearer $accessToken")
            .patch(body)
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw driveApiException(response.code, response.body?.string())
            val responseBody = response.body?.string() ?: throw IOException("Empty response from Drive update")
            gson.fromJson(responseBody, DriveFileResponse::class.java).toDriveBackupInfo()
        }
    }

    private fun driveApiException(code: Int, body: String?): IOException {
        val hint = if (code == 401) " (access token expired — sign out and sign in again)" else ""
        return IOException("Drive API error $code$hint: $body")
    }

    private data class DriveFileListResponse(
        @SerializedName("files") val files: List<DriveFileResponse> = emptyList(),
    )

    private data class DriveFileResponse(
        @SerializedName("id") val id: String = "",
        @SerializedName("modifiedTime") val modifiedTime: String = "",
        @SerializedName("size") val size: String = "0",
    ) {
        fun toDriveBackupInfo() = DriveBackupInfo(
            fileId = id,
            modifiedTimeMs = runCatching { Instant.parse(modifiedTime).toEpochMilli() }.getOrDefault(0L),
            sizeBytes = size.toLongOrNull() ?: 0L,
        )
    }

    private companion object {
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        const val BACKUP_FILE_NAME = "daily_macros_backup.tar"
    }
}
