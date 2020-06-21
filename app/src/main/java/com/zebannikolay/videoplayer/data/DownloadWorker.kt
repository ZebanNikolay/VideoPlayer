package com.zebannikolay.videoplayer.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.zebannikolay.videoplayer.R
import kotlinx.coroutines.delay

class DownloadWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    override suspend fun doWork(): Result {
        val inputUrl = inputData.getString(KEY_INPUT_URL) ?: return Result.failure()
        val outputFile = inputData.getString(KEY_OUTPUT_FILE_NAME) ?: return Result.failure()
        setForeground(createForegroundInfo(0))
        setProgress(workDataOf(KEY_PROGRESS to 0))
        download(inputUrl, outputFile)
        setProgress(workDataOf(KEY_IS_FINISHED to true))
        return Result.success()
    }

    private suspend fun download(inputUrl: String, outputFile: String) {
        for (i in 1..10) {
            delay(1000)
            setProgress(workDataOf(KEY_PROGRESS to i * 10))
            setForeground(createForegroundInfo(i * 10))
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
            .setProgress(100, progress, false)
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
        const val KEY_IS_FINISHED = "KEY_IS_FINISHED"

        private const val DOWNLOAD_CHANNEL_ID = "DOWNLOAD_CHANNEL_ID_1"
        private const val NOTIFICATION_ID = 342
    }
}