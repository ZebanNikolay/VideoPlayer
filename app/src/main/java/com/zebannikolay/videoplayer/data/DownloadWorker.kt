package com.zebannikolay.videoplayer.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.zebannikolay.videoplayer.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception


class DownloadWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    override suspend fun doWork(): Result {
        val inputUrl = inputData.getString(KEY_INPUT_URL) ?: return Result.failure()
        val outputFile = inputData.getString(KEY_OUTPUT_FILE_NAME) ?: return Result.failure()
        setProgress(workDataOf(KEY_PROGRESS to 0))
        setForeground(createForegroundInfo(0))
        val file = File(applicationContext.filesDir, outputFile)
        try {
            download(inputUrl, file)
        } catch (e: Exception) {
            file.delete()
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to e.message))
        }
        return Result.success()
    }

    private suspend fun download(inputUrl: String, outputFile: File) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(inputUrl)
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        val inputStream = response.body?.byteStream()
        saveStreamToFile(inputStream!!, outputFile, response.body!!.contentLength())
    }

    private suspend fun saveStreamToFile(fileInputStream: InputStream, file: File, contentLength: Long) {
        val readBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var downloaded = 0L
        var downloadedPercent = 0
        FileOutputStream(file).run {
            var read = fileInputStream.read(readBuffer)
            while (read != -1) {
                write(readBuffer, 0, read)
                read = fileInputStream.read(readBuffer)
                downloaded += read

                val newPercent = (downloaded / (contentLength / 100)).toInt()
                if (downloadedPercent < newPercent) {
                    downloadedPercent = newPercent
                    setProgress(workDataOf(KEY_PROGRESS to downloadedPercent))
                    setForeground(createForegroundInfo(downloadedPercent))
                }
            }
            flush()
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val title = applicationContext.getString(R.string.notification_title)
        val cancel = applicationContext.getString(R.string.cancel_download)
        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, DOWNLOAD_CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(MAX_PROGRESS, progress, false)
            .addAction(android.R.drawable.ic_delete, cancel, cancelIntent)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            DOWNLOAD_CHANNEL_ID,
            applicationContext.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_INPUT_URL = "KEY_INPUT_URL"
        const val KEY_OUTPUT_FILE_NAME = "KEY_OUTPUT_FILE_NAME"
        const val KEY_PROGRESS = "KEY_PROGRESS"
        const val KEY_ERROR_MESSAGE = "KEY_ERROR"

        private const val DOWNLOAD_CHANNEL_ID = "DOWNLOAD_CHANNEL_ID_1"
        private const val NOTIFICATION_ID = 342
        private const val MAX_PROGRESS = 100
    }
}