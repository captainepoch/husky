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
import androidx.media3.common.MediaItem.Builder
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.keylesspalace.tusky.ViewMediaActivity
import com.keylesspalace.tusky.databinding.FragmentViewVideoBinding
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.util.visible
import timber.log.Timber

@UnstableApi
class ViewVideoFragment : ViewMediaFragment() {

    // TODO(ViewBinding): Remove lateinit in favor of the extension
    // private val binding by viewBinding(FragmentViewVideoBinding::bind)
    private lateinit var binding: FragmentViewVideoBinding

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
        binding.mediaDescription.text = description
        binding.mediaDescription.visible(showingDescription)

        binding.videoView.transitionName = url
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
                binding.videoView.player = player

                val mediaItem = Builder()
                    .setUri(Uri.parse(url))
                    .build()
                player.setMediaItem(mediaItem)

                player.addListener(playbackStateListener)
                player.seekTo(currentWindow, playbackPosition)
                player.playWhenReady = playWhenReady

                player.prepare()
            }

        binding.videoControls.player = binding.videoView.player

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
        mediaActivity = (activity as ViewMediaActivity)
        toolbar = mediaActivity.getToolbar()
        binding = FragmentViewVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val attachment = arguments?.getParcelable<Attachment>(ARG_ATTACHMENT)
            ?: throw IllegalArgumentException("attachment has to be set")

        isAudio = (attachment.type == Attachment.Type.AUDIO)
        finalizeViewSetup(attachment.url, attachment.previewUrl, attachment.description)
    }

    override fun onToolbarVisibilityChange(visible: Boolean) {
        if (!userVisibleHint) {
            return
        }

        isDescriptionVisible = showingDescription && visible
        val alpha = if (isDescriptionVisible) 1.0f else 0.0f
        if (isDescriptionVisible) {
            // If to be visible, need to make visible immediately and animate alpha
            binding.mediaDescription.alpha = 0.0f
            binding.mediaDescription.visible(isDescriptionVisible)
        }

        binding.mediaDescription.animate().alpha(alpha)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.mediaDescription.visible(isDescriptionVisible)
                    animation.removeListener(this)
                }
            })
            .start()

        if (visible && (binding.videoView.player?.isPlaying == true) && !isAudio) {
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
                    binding.progressBar.visibility = View.VISIBLE
                }
                Player.STATE_READY,
                Player.STATE_ENDED -> {
                    binding.progressBar.visibility = View.GONE
                    binding.videoControls.show()
                    binding.videoControls.visibility = View.VISIBLE
                }
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.e(error.errorCodeName)
        }
    }
}
