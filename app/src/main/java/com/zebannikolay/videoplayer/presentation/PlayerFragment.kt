package com.zebannikolay.videoplayer.presentation

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import com.zebannikolay.videoplayer.R
import com.zebannikolay.videoplayer.databinding.PlayerFragmentBinding


private const val APP_NAME = "VideoPlayer"

class PlayerFragment : Fragment() {

    companion object {
        fun newInstance() = PlayerFragment()
    }

    private val eventListener: Player.EventListener = object : Player.EventListener {
        override fun onPlayerError(error: ExoPlaybackException) {
            onError(error)
        }
    }

    private lateinit var viewModel: PlayerViewModel
    private lateinit var binding: PlayerFragmentBinding

    private var player: SimpleExoPlayer? = null
    private lateinit var currentMediaSource: MediaSource

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PlayerFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(PlayerViewModel::class.java)
        binding.vm = viewModel
        observeData()
    }

    private fun observeData() {
        viewModel.playWhenReady.observe(viewLifecycleOwner, Observer { playWhenReady ->
            val uri: Uri = viewModel.getMediaSourceUri() ?: return@Observer
            if (playWhenReady == true && !isSameSource(uri)) {
                updateMediaSource(uri)
            }
            player?.playWhenReady = playWhenReady
        })
        viewModel.errorEvent.observe(viewLifecycleOwner, Observer {
            onError(it)
        })
        viewModel.messageEvent.observe(viewLifecycleOwner, Observer {
            if (it.isNullOrBlank()) return@Observer
            showMessage(it)
        })
    }

    private fun isSameSource(uri: Uri): Boolean {
        return currentMediaSource.tag == uri.toString()
    }

    private fun updateMediaSource(uri: Uri) {
        currentMediaSource = createMediaSource(uri)
        player?.prepare(currentMediaSource)
    }

    private fun initPlayer() {
        player = SimpleExoPlayer.Builder(requireContext()).build()
        binding.player.player = player
        player?.addListener(eventListener)
        currentMediaSource = createMediaSource(viewModel.getMediaSourceUri() ?: return)
        player?.seekTo(viewModel.currentPosition.value ?: 0)
        player?.prepare(currentMediaSource, false, false)
    }

    private fun createMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            requireContext(),
            Util.getUserAgent(requireContext(), APP_NAME)
        )
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .setTag(uri.toString())
            .createMediaSource(uri)
    }

    private fun releasePlayer() {
        player?.let {
            viewModel.playWhenReady.value = it.playWhenReady
            viewModel.currentPosition.value = it.currentPosition
            player?.removeListener(eventListener)
            it.release()
            player = null
        }
    }

    private fun onError(throwable: Throwable?) {
        viewModel.playWhenReady.postValue(false)
        showMessage(throwable?.message ?: getString(R.string.error))
    }

    private fun showMessage(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}