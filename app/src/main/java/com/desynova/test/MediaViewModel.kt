package com.desynova.test

import androidx.lifecycle.ViewModel

class MediaViewModel : ViewModel() {

    var playWhenReady = true
    var currentWindow = 0
    var playbackPosition: Long = 0
}