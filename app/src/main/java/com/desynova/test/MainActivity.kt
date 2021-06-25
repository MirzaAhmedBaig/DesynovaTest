package com.desynova.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.desynova.test.databinding.ActivityMainBinding
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Util


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var exoPlayer: SimpleExoPlayer? = null
    private var currentLang = 0

    private var allLanguages: ArrayList<Pair<String, String>>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.bvLanguage.setOnClickListener {
            trackSelector?.let {
                it.setParameters(
                    it.parameters.buildUpon()
                        .setPreferredAudioLanguage(allLanguages!![currentLang].second)
                )
                Log.d(TAG, "New lang is ${allLanguages!![currentLang].second}")
                currentLang = (currentLang + 1) % 2
            }

        }
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

    private fun initializePlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        binding.exoPlayerView.player = exoPlayer
        binding.exoPlayerView.useController = true
        exoPlayer?.playWhenReady = true

        val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3))
        exoPlayer?.setMediaItem(mediaItem)

        exoPlayer?.playWhenReady = playWhenReady
        exoPlayer?.seekTo(currentWindow, playbackPosition)
        exoPlayer?.prepare()


        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                if (state == Player.STATE_READY) {
                    allLanguages = ArrayList()
                    for (i in 0 until exoPlayer!!.currentTrackGroups.length) {
                        val format =
                            exoPlayer?.currentTrackGroups?.get(i)?.getFormat(0)?.sampleMimeType
                        val lang = exoPlayer?.currentTrackGroups?.get(i)?.getFormat(0)?.language
                        val id = exoPlayer?.currentTrackGroups?.get(i)?.getFormat(0)?.id
                        Log.d(TAG, "${exoPlayer?.currentTrackGroups?.get(i)?.getFormat(0)}")
                        if (format!!.contains("audio") && id != null && lang != null) {
                            Log.d(TAG, "$lang $id")
                            allLanguages?.add(Pair(id, lang))
                        }
                    }
                    trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector
                }
            }

        })


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


    fun setAudioTrack(track: Int) {
        println("setAudioTrack: $track")
        val mappedTrackInfo: MappedTrackInfo =
            Assertions.checkNotNull(trackSelector?.currentMappedTrackInfo)
        val parameters: DefaultTrackSelector.Parameters = trackSelector?.parameters ?: return
        val builder = parameters.buildUpon()
        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            val trackType = mappedTrackInfo.getRendererType(rendererIndex)
            if (trackType == C.TRACK_TYPE_AUDIO) {
                builder.clearSelectionOverrides(rendererIndex)
                    .setRendererDisabled(rendererIndex, false)
                val groupIndex = track - 1
                val tracks = intArrayOf(0)
                val override = SelectionOverride(groupIndex, *tracks)
                builder.setSelectionOverride(
                    rendererIndex,
                    mappedTrackInfo.getTrackGroups(rendererIndex),
                    override
                )
            }
        }
        trackSelector?.setParameters(builder)
//        curentAudioTrack = track
    }
}