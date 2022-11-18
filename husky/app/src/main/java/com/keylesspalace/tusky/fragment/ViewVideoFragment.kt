/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2017  Andrew Dawson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.keylesspalace.tusky.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.ViewMediaActivity
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.util.visible
import kotlinx.android.synthetic.main.activity_view_media.toolbar
import kotlinx.android.synthetic.main.fragment_view_video.mediaDescription
import kotlinx.android.synthetic.main.fragment_view_video.progressBar
import kotlinx.android.synthetic.main.fragment_view_video.videoControls
import kotlinx.android.synthetic.main.fragment_view_video.videoView
import timber.log.Timber

class ViewVideoFragment : ViewMediaFragment() {

    private lateinit var toolbar: View
    private val handler = Handler(Looper.getMainLooper())
    private val hideToolbar = Runnable {
        // Hoist toolbar hiding to activity so it can track state across different fragments
        // This is explicitly stored as runnable so that we pass it to the handler later for cancellation
        mediaActivity.onPhotoTap()
        mediaController.hide()
    }
    private lateinit var mediaActivity: ViewMediaActivity
    private val TOOLBAR_HIDE_DELAY_MS = 3000L
    private lateinit var mediaController: MediaController
    private var isAudio = false

    private var exoPlayer: ExoPlayer? = null
    private val playbackStateListener: Player.Listener = playbackStateListener()
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        // Start/pause/resume video playback as fragment is shown/hidden
        super.setUserVisibleHint(isVisibleToUser)

        if (videoView == null) {
            return
        }

        if (isVisibleToUser) {
            if (mediaActivity.isToolbarVisible) {
                handler.postDelayed(hideToolbar, TOOLBAR_HIDE_DELAY_MS)
            }
            exoPlayer?.play()
        } else {
            handler.removeCallbacks(hideToolbar)
            exoPlayer?.pause()
            mediaController.hide()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setupMediaView(
        url: String,
        previewUrl: String?,
        description: String?,
        showingDescription: Boolean
    ) {
        mediaDescription.text = description
        mediaDescription.visible(showingDescription)

        videoView.transitionName = url
        mediaController = object : MediaController(mediaActivity) {
            override fun show(timeout: Int) {
                // We're doing manual auto-close management.
                // Also, take focus back from the pause button so we can use the back button.
                super.show(0)
                mediaController.requestFocus()
            }

            override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) {
                        hide()
                        activity?.supportFinishAfterTransition()
                    }
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }

        val trackSelector = DefaultTrackSelector(requireActivity()).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        exoPlayer = ExoPlayer.Builder(requireActivity())
            .setUsePlatformDiagnostics(false)
            .setTrackSelector(trackSelector)
            .build()
            .also { player ->
                videoView.player = player

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(url))
                    .build()
                player.setMediaItem(mediaItem)

                player.addListener(playbackStateListener)
                player.seekTo(currentWindow, playbackPosition)
                player.playWhenReady = playWhenReady

                player.prepare()
            }

        videoControls.player = videoView.player

        if (arguments!!.getBoolean(ARG_START_POSTPONED_TRANSITION)) {
            mediaActivity.onBringUp()
        }
    }

    private fun hideToolbarAfterDelay(delayMilliseconds: Long) {
        handler.postDelayed(hideToolbar, delayMilliseconds)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        toolbar = activity!!.toolbar
        mediaActivity = activity as ViewMediaActivity
        return inflater.inflate(R.layout.fragment_view_video, container, false)
    }

    override fun onStart() {
        super.onStart()

        val attachment = arguments?.getParcelable<Attachment>(ARG_ATTACHMENT)
            ?: throw IllegalArgumentException("attachment has to be set")

        isAudio = (attachment.type == Attachment.Type.AUDIO)
        finalizeViewSetup(attachment.url, attachment.previewUrl, attachment.description)
    }

    override fun onToolbarVisibilityChange(visible: Boolean) {
        if (videoView == null || mediaDescription == null || !userVisibleHint) {
            return
        }

        isDescriptionVisible = showingDescription && visible
        val alpha = if (isDescriptionVisible) 1.0f else 0.0f
        if (isDescriptionVisible) {
            // If to be visible, need to make visible immediately and animate alpha
            mediaDescription.alpha = 0.0f
            mediaDescription.visible(isDescriptionVisible)
        }

        mediaDescription.animate().alpha(alpha)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mediaDescription?.visible(isDescriptionVisible)
                    animation.removeListener(this)
                }
            })
            .start()

        if (visible && (videoView.player?.isPlaying == true) && !isAudio) {
            hideToolbarAfterDelay(TOOLBAR_HIDE_DELAY_MS)
        } else {
            handler.removeCallbacks(hideToolbar)
        }
    }

    override fun onTransitionEnd() {
    }

    override fun onPause() {
        super.onPause()

        releasePlayer()
    }

    override fun onStop() {
        super.onStop()

        releasePlayer()
    }

    private fun releasePlayer() {
        exoPlayer?.run {
            playbackPosition = this.currentPosition
            currentWindow = this.currentWindowIndex
            playWhenReady = this.playWhenReady
            removeListener(playbackStateListener)
            release()
        }
        exoPlayer = null
    }

    private fun playbackStateListener() = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    progressBar.visibility = View.VISIBLE
                }
                Player.STATE_READY,
                Player.STATE_ENDED -> {
                    progressBar.visibility = View.GONE
                    videoControls.show()
                    videoControls.visibility = View.VISIBLE
                }
                else -> {
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.e(error.errorCodeName)
        }
    }
}
