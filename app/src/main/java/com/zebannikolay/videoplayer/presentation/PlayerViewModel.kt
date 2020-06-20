package com.zebannikolay.videoplayer.presentation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayerViewModel : ViewModel() {

    val uriPath: MutableLiveData<String> = MutableLiveData("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")

    val playWhenReady: MutableLiveData<Boolean> = MutableLiveData(false)
    val currentPosition: MutableLiveData<Long> = MutableLiveData(0)
}