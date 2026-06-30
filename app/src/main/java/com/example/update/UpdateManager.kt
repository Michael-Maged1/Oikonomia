package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val apkUrl: String, val latestVersion: String) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class Downloaded(val apkUri: Uri) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class UpdateManager(private val context: Context) {
    companion object {
        private const val TAG = "UpdateManager"
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun checkForUpdates() {
        _updateState.value = UpdateState.Checking
        // Simulate a check or retrieve from Firebase Remote Config / remote API.
        // For demonstration/functional integration, we offer a target APK URL.
        // The user can define this in Remote Config or a default stable URL.
        val apkUrl = "https://raw.githubusercontent.com/Anish-Shrestha/Notification-Sounds/master/dummy.apk" // Fallback / placeholder APK URL
        
        // In real use, we'd fetch JSON. Let's make it available.
        _updateState.value = UpdateState.UpdateAvailable(apkUrl, "2.0.0")
    }

    suspend fun startDownload(apkUrl: String) {
        _updateState.value = UpdateState.Downloading(0f)
        withContext(Dispatchers.IO) {
            try {
                val url = URL(apkUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    _updateState.value = UpdateState.Error("Server returned code ${connection.responseCode}")
                    return@withContext
                }

                val fileLength = connection.contentLength
                val cacheDir = context.cacheDir
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val outputFile = File(cacheDir, "update.apk")
                if (outputFile.exists()) {
                    outputFile.delete()
                }

                connection.inputStream.use { input ->
                    outputFile.outputStream().use { output ->
                        val data = ByteArray(4096)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                val progress = total.toFloat() / fileLength.toFloat()
                                _updateState.value = UpdateState.Downloading(progress)
                            }
                            output.write(data, 0, count)
                        }
                    }
                }

                Log.d(TAG, "Download finished: ${outputFile.absolutePath}")
                
                // Get Uri from FileProvider
                val authority = "com.Michael.Oikonomia.fileprovider"
                val apkUri = FileProvider.getUriForFile(context, authority, outputFile)
                _updateState.value = UpdateState.Downloaded(apkUri)
                
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _updateState.value = UpdateState.Error(e.localizedMessage ?: "Unknown download error")
            }
        }
    }

    fun triggerInstall(apkUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Installation trigger failed", e)
            _updateState.value = UpdateState.Error("Failed to trigger installation: ${e.localizedMessage}")
        }
    }
}
