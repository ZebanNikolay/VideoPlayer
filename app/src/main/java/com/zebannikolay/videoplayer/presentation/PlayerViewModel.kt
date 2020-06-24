package com.zebannikolay.videoplayer.presentation

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.*
import com.zebannikolay.videoplayer.R
import com.zebannikolay.videoplayer.data.DownloadWorker
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val sharedPref = context.getSharedPreferences(
        context.getString(R.string.preference_file_key), Context.MODE_PRIVATE
    )

    val url: MutableLiveData<String> =
        MutableLiveData(context.getString(R.string.video_url))

    val playWhenReady: MutableLiveData<Boolean> = MutableLiveData(false)
    val currentPosition: MutableLiveData<Long> = MutableLiveData(0)

    val progress: MutableLiveData<Int> = MutableLiveData(0)
    val isDownloading: MutableLiveData<Boolean> = MutableLiveData(false)

    val errorEvent: MutableLiveData<Exception> = MutableLiveData()
    val messageEvent: MutableLiveData<String> = MutableLiveData()

    private val workManager by lazy(LazyThreadSafetyMode.NONE) {
        WorkManager.getInstance(context)
    }

    init {
        url.observeForever {
            playWhenReady.value = false
        }
    }

    fun onDownloadClicked() {
        if (isFileDownloaded()) {
            messageEvent.value =
                context.getString(R.string.file_already_downloaded)
            return
        }
        downloadFile()
    }

    private fun isFileDownloaded(): Boolean {
        return sharedPref.getString(url.value, null) != null
    }

    private fun downloadFile() {
        isDownloading.value = true
        progress.value = 0
        val fileName = createFileName()
        val request: WorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_INPUT_URL to url.value,
                    DownloadWorker.KEY_OUTPUT_FILE_NAME to fileName
                )
            )
            .build()
        workManager.enqueue(request)
        observeProgress(request, fileName)
    }

    private fun createFileName(): String {
        val uri = try {
            Uri.parse(url.value)
        } catch (e: Exception) {
            errorEvent.value = e
            return ""
        }
        return UUID.randomUUID().toString() + "_" + uri.lastPathSegment
    }

    private fun observeProgress(request: WorkRequest, fileName: String) {
        val workLiveData = workManager.getWorkInfoByIdLiveData(request.id)
        workLiveData.observeForever(object : Observer<WorkInfo> {
            override fun onChanged(workInfo: WorkInfo?) {
                workInfo ?: return
                progress.value = workInfo.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    onFileDownloadSucceeded(fileName)
                }
                if (workInfo.state == WorkInfo.State.FAILED) {
                    onFileDownloadFailed(workInfo)
                }
                if (workInfo.state.isFinished) {
                    isDownloading.value = false
                    workLiveData.removeObserver(this)
                }
            }
        })
    }

    private fun onFileDownloadSucceeded(fileName: String) {
        sharedPref.edit()
            .putString(url.value, fileName)
            .apply()
        messageEvent.value =
            getApplication<Application>().getString(R.string.file_downloaded)
    }

    private fun onFileDownloadFailed(workInfo: WorkInfo) {
        val errorMessage =
            workInfo.outputData.getString(DownloadWorker.KEY_ERROR_MESSAGE)
        errorEvent.value = IOException(errorMessage)
    }

    fun getMediaSourceUri(): Uri? {
        val savedFileName = sharedPref.getString(url.value, null)
        return try {
            if (savedFileName == null) {
                Uri.parse(url.value)
            } else {
                Uri.fromFile(File(context.filesDir, savedFileName))
            }
        } catch (e: Exception) {
            errorEvent.value = e
            return null
        }
    }
}