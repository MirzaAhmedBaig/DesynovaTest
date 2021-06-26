package com.desynova.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.desynova.test.databinding.ActivityMainBinding
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.Util


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var exoPlayer: SimpleExoPlayer? = null
    private var allLanguages: ArrayList<Pair<String, String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setListeners()
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT < 24 || exoPlayer == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        binding.exoPlayerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    private fun setListeners() {
        binding.bvTrack.setOnClickListener {
            langPopupWindow.showAsDropDown(it)
            return@setOnClickListener
        }

        binding.bvPayerState.setOnClickListener {
            Log.d(TAG, "exoPlayer?.playbackState: ${exoPlayer?.playbackState}")
            when (exoPlayer?.playbackState) {
                STATE_ENDED -> {
                    exoPlayer?.seekTo(0)
                    exoPlayer?.playWhenReady = true
                }
                else -> {
                    if (exoPlayer?.isPlaying == true)
                        exoPlayer?.pause()
                    else
                        exoPlayer?.play()
                }
            }
        }

        binding.youtubeOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationStart() {
                binding.youtubeOverlay.visibility = View.VISIBLE
            }

            override fun onAnimationEnd() {
                binding.youtubeOverlay.visibility = View.GONE
            }
        })
    }


    private fun initializePlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        with(binding.exoPlayerView) {
            this.player = exoPlayer
            this.useController = true
            this.setControllerVisibilityListener {
                binding.bvPayerState.visibility = it
                if (!allLanguages.isNullOrEmpty())
                    binding.bvTrack.visibility = it
            }

        }

        with(exoPlayer) {
            this?.setMediaItem(MediaItem.fromUri(getString(R.string.media_url_mp3)))
            this?.playWhenReady = playWhenReady
            this?.seekTo(currentWindow, playbackPosition)
            this?.prepare()
            this?.addListener(object : Player.Listener {

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    Log.d(TAG, "onIsPlayingChanged $isPlaying")
                    updatePlayerState(isPlaying)
                }

                override fun onPlaybackStateChanged(state: Int) {
                    super.onPlaybackStateChanged(state)
                    Log.d(TAG, "Srtate : $state")
                    if (state == STATE_ENDED)
                        updatePlayerState(false)
                    if (state == Player.STATE_READY) {
                        allLanguages = ArrayList()
                        for (i in 0 until exoPlayer!!.currentTrackGroups.length) {
                            val format =
                                exoPlayer?.currentTrackGroups?.get(i)?.getFormat(0)?.sampleMimeType
                            val lang = exoPlayer?.currentTrackGroups?.get(i)?.getFormat(0)?.language
                            val id = exoPlayer?.currentTrackGroups?.get(i)?.getFormat(0)?.id
                            if (format!!.contains("audio") && id != null && lang != null) {
                                allLanguages?.add(Pair(id, lang))
                            }
                        }
                        if (!allLanguages.isNullOrEmpty())
                            binding.bvTrack.visibility = View.VISIBLE
                        this@MainActivity.trackSelector =
                            exoPlayer?.trackSelector as? DefaultTrackSelector
                    }
                }
            })
        }

        binding.youtubeOverlay.player(exoPlayer!!)

    }

    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var trackSelector: DefaultTrackSelector? = null
    private fun releasePlayer() {
        exoPlayer?.let {
            playWhenReady = it.playWhenReady
            playbackPosition = it.currentPosition
            currentWindow = it.currentWindowIndex
            it.release()
            exoPlayer = null
        }

    }

    private fun updatePlayerState(isPlaying: Boolean) {
        val image = if (isPlaying) {
            R.drawable.ic_pause
        } else {
            if (exoPlayer?.playbackState == STATE_ENDED)
                R.drawable.ic_replay
            else
                R.drawable.ic_play
        }
        if (exoPlayer?.playbackState == STATE_BUFFERING) {
            binding.pbLoader.visibility = View.VISIBLE
            binding.bvPayerState.visibility = View.GONE
        } else {
            binding.pbLoader.visibility = View.GONE
            binding.bvPayerState.visibility = View.VISIBLE
        }
        binding.bvPayerState.setImageResource(image)
    }

    private val langPopupWindow by lazy {
        PopupWindow(this).apply {
            isFocusable = true
            width = 150.dpToPx()
            height = WindowManager.LayoutParams.WRAP_CONTENT
            contentView = ListView(this@MainActivity).apply {
                adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    allLanguages!!.map { it.second })

                this.setOnItemClickListener { parent, view, position, id ->
                    trackSelector?.let {
                        it.setParameters(
                            it.parameters.buildUpon()
                                .setPreferredAudioLanguage(allLanguages!![position].second)
                        )
                        Toast.makeText(
                            this@MainActivity,
                            allLanguages!![position].second,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dismiss()
                }
            }
        }
    }


}