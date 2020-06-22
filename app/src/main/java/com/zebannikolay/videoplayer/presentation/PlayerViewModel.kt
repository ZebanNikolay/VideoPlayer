package com.zebannikolay.videoplayer.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.*
import com.zebannikolay.videoplayer.data.DownloadWorker
import java.io.IOException
import java.lang.Exception

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val uriPath: MutableLiveData<String> =
        MutableLiveData("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")

    val playWhenReady: MutableLiveData<Boolean> = MutableLiveData(false)
    val currentPosition: MutableLiveData<Long> = MutableLiveData(0)

    val progress: MutableLiveData<Int> = MutableLiveData(0)
    val isDownloading: MutableLiveData<Boolean> = MutableLiveData(false)
    val errorEvent: MutableLiveData<Exception> = MutableLiveData()

    private val workManager by lazy(LazyThreadSafetyMode.NONE) {
        WorkManager.getInstance(getApplication())
    }

    fun onDownloadClicked() {
        isDownloading.value = true
        progress.value = 0
        val request: WorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_INPUT_URL to uriPath.value,
                    DownloadWorker.KEY_OUTPUT_FILE_NAME to "test"
                )
            )
            .build()
        workManager.enqueue(request)
        observeProgress(request)
    }

    private fun observeProgress(request: WorkRequest) {
        val workLiveData = workManager.getWorkInfoByIdLiveData(request.id)
        workLiveData.observeForever(object : Observer<WorkInfo> {
            override fun onChanged(workInfo: WorkInfo?) {
                workInfo ?: return
                val data = workInfo.progress
                val value = data.getInt(DownloadWorker.KEY_PROGRESS, 0)
                progress.value = value
                if (workInfo.state.isFinished) {
                    isDownloading.value = false
                    workLiveData.removeObserver(this)
                }
                if (workInfo.state == WorkInfo.State.FAILED) {
                    val errorMessage = workInfo.outputData.getString(DownloadWorker.KEY_ERROR_MESSAGE)
                    errorEvent.value = IOException(errorMessage)
                }
            }
        })
    }
}