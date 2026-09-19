package com.rizal.radiotune.data.update

import android.content.Context
import com.rizal.radiotune.data.remote.RemoteConfig
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ApkDownloader(
    baseClient: OkHttpClient,
    private val context: Context,
) {

    private val client = baseClient.newBuilder()
        .cache(null)
        .callTimeout(5, TimeUnit.MINUTES)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Downloads the first reachable [urls] candidate, reporting whole-percent
     * progress. The APK lands in app-specific storage, which needs no
     * storage permission on any supported Android version.
     */
    suspend fun download(urls: List<String>, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            var lastError: IOException? = null
            for (url in urls) {
                try {
                    return@withContext downloadFrom(url, onProgress)
                } catch (error: IOException) {
                    lastError = error
                }
            }
            throw lastError ?: IOException("No download URL available")
        }

    private fun downloadFrom(url: String, onProgress: (Int) -> Unit): File {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", RemoteConfig.USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} for $url")
            }
            val body = response.body ?: throw IOException("Empty response body")

            val directory = File(
                context.getExternalFilesDir(null) ?: context.filesDir,
                UPDATE_DIRECTORY,
            ).apply { mkdirs() }
            val target = File(directory, UPDATE_FILE_NAME)

            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val total = body.contentLength()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    var read = input.read(buffer)
                    while (read >= 0) {
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) {
                            onProgress(((copied * 100) / total).toInt().coerceIn(0, 100))
                        }
                        read = input.read(buffer)
                    }
                }
            }

            if (target.length() == 0L) {
                throw IOException("Downloaded file is empty")
            }
            onProgress(100)
            return target
        }
    }

    companion object {
        const val UPDATE_DIRECTORY = "updates"
        const val UPDATE_FILE_NAME = "RadioTune-update.apk"
    }
}
