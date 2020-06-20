package com.zebannikolay.videoplayer.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.zebannikolay.videoplayer.R

class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.player_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, PlayerFragment.newInstance())
                .commitNow()
        }
    }
}