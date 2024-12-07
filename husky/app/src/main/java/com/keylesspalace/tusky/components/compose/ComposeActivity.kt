/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2019  Tusky Contributors
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

package com.keylesspalace.tusky.components.compose

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.keylesspalace.tusky.BaseActivity
import com.keylesspalace.tusky.BuildConfig
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.adapter.EmojiAdapter
import com.keylesspalace.tusky.adapter.OnEmojiSelectedListener
import com.keylesspalace.tusky.appstore.Event
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.StatusPreviewEvent
import com.keylesspalace.tusky.components.common.AudioSizeException
import com.keylesspalace.tusky.components.common.ImageSizeException
import com.keylesspalace.tusky.components.common.MediaSizeException
import com.keylesspalace.tusky.components.common.VideoOrImageException
import com.keylesspalace.tusky.components.common.VideoSizeException
import com.keylesspalace.tusky.components.common.createNewImageFile
import com.keylesspalace.tusky.components.common.toFileName
import com.keylesspalace.tusky.components.compose.dialog.makeCaptionDialog
import com.keylesspalace.tusky.components.compose.dialog.showAddPollDialog
import com.keylesspalace.tusky.components.compose.view.ComposeOptionsListener
import com.keylesspalace.tusky.core.extensions.afterTextChanged
import com.keylesspalace.tusky.core.extensions.composeWithZwsp
import com.keylesspalace.tusky.core.extensions.onTextChanged
import com.keylesspalace.tusky.core.extensions.toMB
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.core.utils.InstanceConstants
import com.keylesspalace.tusky.databinding.ActivityComposeBinding
import com.keylesspalace.tusky.db.AccountEntity
import com.keylesspalace.tusky.db.DraftAttachment
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.entity.NewPoll
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.BBCodeEdit
import com.keylesspalace.tusky.util.ComposeTokenizer
import com.keylesspalace.tusky.util.HTMLEdit
import com.keylesspalace.tusky.util.PostFormat
import com.keylesspalace.tusky.util.PostFormat.BBCODE
import com.keylesspalace.tusky.util.PostFormat.HTML
import com.keylesspalace.tusky.util.PostFormat.MARKDOWN
import com.keylesspalace.tusky.util.ThemeUtils
import com.keylesspalace.tusky.util.combineLiveData
import com.keylesspalace.tusky.util.combineOptionalLiveData
import com.keylesspalace.tusky.util.hide
import com.keylesspalace.tusky.util.highlightSpans
import com.keylesspalace.tusky.util.loadAvatar
import com.keylesspalace.tusky.util.observeOnce
import com.keylesspalace.tusky.util.show
import com.keylesspalace.tusky.util.visible
import com.keylesspalace.tusky.util.withLifecycleContext
import com.keylesspalace.tusky.view.EmojiKeyboard
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.uber.autodispose.android.lifecycle.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlinx.parcelize.Parcelize
import me.thanel.markdownedit.MarkdownEdit
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ComposeActivity :
    BaseActivity(),
    ComposeOptionsListener,
    ComposeAutoCompleteAdapter.AutocompletionProvider,
    OnEmojiSelectedListener,
    InputConnectionCompat.OnCommitContentListener,
    TimePickerDialog.OnTimeSetListener,
    EmojiKeyboard.OnEmojiSelectedListener {

    private val binding by viewBinding(ActivityComposeBinding::inflate)
    private val viewModel: ComposeViewModel by viewModel()
    private val eventHub: EventHub by inject()

    private lateinit var composeOptionsBehavior: BottomSheetBehavior<*>
    private lateinit var addMediaBehavior: BottomSheetBehavior<*>
    private lateinit var emojiBehavior: BottomSheetBehavior<*>
    private lateinit var scheduleBehavior: BottomSheetBehavior<*>
    private lateinit var stickerBehavior: BottomSheetBehavior<*>
    private lateinit var previewBehavior: BottomSheetBehavior<*>

    // this only exists when a status is trying to be sent, but uploads are still occurring
    private var finishingUploadDialog: ProgressDialog? = null
    private var photoUploadUri: Uri? = null
    @VisibleForTesting
    var maximumTootCharacters = InstanceConstants.DEFAULT_CHARACTER_LIMIT
    private var suggestFormattingSyntax: String = "text/markdown"
    private var mediaCount = 0
    private val preferences: SharedPreferences by inject()

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
            // Acting like a teen: deliberately ignoring parent.
            if (composeOptionsBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
                addMediaBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
                emojiBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
                scheduleBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
                stickerBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
                previewBehavior.state == BottomSheetBehavior.STATE_EXPANDED
            ) {
                composeOptionsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                scheduleBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                previewBehavior.state = BottomSheetBehavior.STATE_HIDDEN

                return
            }

            handleCloseButton()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val theme = preferences.getString("appTheme", ThemeUtils.APP_THEME_DEFAULT)
        if (theme == "black") {
            setTheme(R.style.TuskyDialogActivityBlackTheme)
        }
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setupActionBar()
        // do not do anything when not logged in, activity will be finished in super.onCreate() anyway
        val activeAccount = accountManager.value.activeAccount ?: return

        viewModel.tryFetchStickers = preferences.getBoolean(PrefKeys.STICKERS, false)
        viewModel.anonymizeNames = preferences.getBoolean(PrefKeys.ANONYMIZE_FILENAMES, false)
        setupAvatar(preferences, activeAccount)
        val mediaAdapter = MediaPreviewAdapter(
            this,
            onAddCaption = { item ->
                makeCaptionDialog(item.description, item.uri) { newDescription ->
                    viewModel.updateDescription(item.localId, newDescription)
                }
            },
            onRemove = this::removeMediaFromQueue
        )
        viewModel.postExpiresIn.value = activeAccount.postExpiresIn.takeIf { it > 0 }

        binding.composeMediaPreviewBar.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.composeMediaPreviewBar.adapter = mediaAdapter
        binding.composeMediaPreviewBar.itemAnimator = null

        // set before subscribing to updates to not accidentally catch it
        viewModel.formattingSyntax.value = activeAccount.defaultFormattingSyntax

        subscribeToUpdates(mediaAdapter, activeAccount)
        setupButtons()

        photoUploadUri = savedInstanceState?.getParcelable(PHOTO_UPLOAD_URI_KEY)

        /* If the composer is started up as a reply to another post, override the "starting" state
         * based on what the intent from the reply request passes. */

        val composeOptions = intent.getParcelableExtra<ComposeOptions?>(COMPOSE_OPTIONS_EXTRA)

        suggestFormattingSyntax = if (!composeOptions?.formattingSyntax.isNullOrEmpty()) {
            composeOptions?.formattingSyntax!!
        } else {
            activeAccount.defaultFormattingSyntax
        }

        viewModel.setup(composeOptions)
        setupReplyViews(composeOptions?.replyingStatusAuthor, composeOptions?.replyingStatusContent)
        val tootText = composeOptions?.tootText
        if (!tootText.isNullOrEmpty()) {
            binding.composeEditField.setText(tootText)
        }

        if (!composeOptions?.scheduledAt.isNullOrEmpty()) {
            binding.composeScheduleView.setDateTime(composeOptions?.scheduledAt)
        }

        viewModel.composeWithZwsp.value =
            preferences.getBoolean(PrefKeys.COMPOSING_ZWSP_CHAR, false)
        viewModel.quotePostId = composeOptions?.quotePostId

        setupComposeField(viewModel.startingText)
        setupContentWarningField(composeOptions?.contentWarning)
        setupPollView()
        applyShareIntent(intent, savedInstanceState)
        viewModel.setupComplete.value = true

        binding.stickerKeyboard.isSticky = true

        eventHub.events.observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe { event: Event? ->
                when (event) {
                    is StatusPreviewEvent -> onStatusPreviewReady(event.status)
                }
            }
    }

    private fun applyShareIntent(intent: Intent, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            // Hack: Instance values are needed before sharing content
            // TODO: Better way to make this flow
            viewModel.instanceParams.observe(this) {
                /* Get incoming images being sent through a share action from another app. Only do this
                 * when savedInstanceState is null, otherwise both the images from the intent and the
                 * instance state will be re-queued.
                 */
                intent.type?.also { type ->
                    if (type.startsWith("image/") || type.startsWith("video/") || type.startsWith("audio/")) {
                        when (intent.action) {
                            Intent.ACTION_SEND -> {
                                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                                    pickMedia(uri)
                                }
                            }

                            Intent.ACTION_SEND_MULTIPLE -> {
                                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                                    ?.forEach { uri ->
                                        pickMedia(uri)
                                    }
                            }
                        }
                    } else if (type == "text/plain" && intent.action == Intent.ACTION_SEND) {
                        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                        val shareBody = if (!subject.isNullOrBlank() && subject !in text) {
                            subject + '\n' + text
                        } else {
                            text
                        }

                        if (shareBody.isNotBlank()) {
                            val start = binding.composeEditField.selectionStart.coerceAtLeast(0)
                            val end = binding.composeEditField.selectionEnd.coerceAtLeast(0)
                            val left = min(start, end)
                            val right = max(start, end)
                            binding.composeEditField.text.replace(
                                left, right, shareBody, 0, shareBody.length
                            )
                            // move edittext cursor to first when shareBody parsed
                            binding.composeEditField.text.insert(0, "\n")
                            binding.composeEditField.setSelection(0)
                        }
                    }
                }
            }
        }
    }

    private fun setupReplyViews(replyingStatusAuthor: String?, replyingStatusContent: String?) {
        if (replyingStatusAuthor != null) {
            binding.composeReplyView.show()
            binding.composeReplyView.text = getString(R.string.replying_to, replyingStatusAuthor)
            val arrowDownIcon =
                IconicsDrawable(this, GoogleMaterial.Icon.gmd_arrow_drop_down).apply { sizeDp = 12 }

            ThemeUtils.setDrawableTint(this, arrowDownIcon, android.R.attr.textColorTertiary)
            binding.composeReplyView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                arrowDownIcon,
                null
            )

            binding.composeReplyView.setOnClickListener {
                TransitionManager.beginDelayedTransition(binding.composeReplyContentView.parent as ViewGroup)

                if (binding.composeReplyContentView.isVisible) {
                    binding.composeReplyContentView.hide()
                    binding.composeReplyView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        arrowDownIcon,
                        null
                    )
                } else {
                    binding.composeReplyContentView.show()
                    val arrowUpIcon = IconicsDrawable(
                        this,
                        GoogleMaterial.Icon.gmd_arrow_drop_up
                    ).apply { sizeDp = 12 }

                    ThemeUtils.setDrawableTint(this, arrowUpIcon, android.R.attr.textColorTertiary)
                    binding.composeReplyView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        arrowUpIcon,
                        null
                    )
                }
            }
        }
        replyingStatusContent?.let { binding.composeReplyContentView.text = it }
    }

    private fun setupContentWarningField(startingContentWarning: String?) {
        if (startingContentWarning != null) {
            binding.composeContentWarningField.setText(startingContentWarning)
        }

        binding.composeContentWarningField.onTextChanged { _, _, _, _ -> updateVisibleCharactersLeft() }
    }

    private fun setupComposeField(startingText: String?) {
        binding.composeEditField.setOnCommitContentListener(this)

        binding.composeEditField.setOnKeyListener { _, keyCode, event ->
            this.onKeyDown(
                keyCode,
                event
            )
        }

        binding.composeEditField.setAdapter(
            ComposeAutoCompleteAdapter(this)
        )
        binding.composeEditField.setTokenizer(ComposeTokenizer())

        binding.composeEditField.setText(startingText)
        binding.composeEditField.setSelection(binding.composeEditField.length())

        val mentionColour = binding.composeEditField.linkTextColors.defaultColor
        highlightSpans(binding.composeEditField.text, mentionColour)
        binding.composeEditField.afterTextChanged { editable ->
            highlightSpans(editable, mentionColour)
            updateVisibleCharactersLeft()
        }

        // work around Android platform bug -> https://issuetracker.google.com/issues/67102093
        if (VERSION.SDK_INT == VERSION_CODES.O ||
            VERSION.SDK_INT == VERSION_CODES.O_MR1
        ) {
            binding.composeEditField.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    private fun reenableAttachments() {
        // in case of we already had disabled attachments
        // but got information about extension later
        enableButton(binding.composeAddMediaButton, clickable = true, colorActive = true)
        enablePollButton(true)
    }

    @VisibleForTesting
    var supportedFormattingSyntax = arrayListOf<String>()

    private fun subscribeToUpdates(
        mediaAdapter: MediaPreviewAdapter,
        activeAccount: AccountEntity
    ) {
        withLifecycleContext {
            viewModel.instanceParams.observe { instanceData ->
                maximumTootCharacters = instanceData.maxChars
                updateVisibleCharactersLeft()
                binding.composeScheduleButton.visible(instanceData.supportsScheduled)
            }

            viewModel.instanceParams.observe { instanceData ->
                if(instanceData.postFormats.isEmpty()) {
                    binding.composeFormattingSyntax.isEnabled = false
                } else {
                    binding.composeFormattingSyntax.isEnabled = true

                    instanceData.postFormats.forEach {
                        supportedFormattingSyntax.add(it.formatValue)
                    }
                }

                if (supportedFormattingSyntax.size != 0) {
                    binding.composeFormattingSyntax.visible(true)

                    val supportsPrefferedSyntax =
                        supportedFormattingSyntax.contains(viewModel.formattingSyntax.value!!)

                    if (!supportsPrefferedSyntax) {
                        suggestFormattingSyntax =
                            if (supportedFormattingSyntax.contains(activeAccount.defaultFormattingSyntax)) {
                                activeAccount.defaultFormattingSyntax
                            } else {
                                supportedFormattingSyntax[0]
                            }

                        viewModel.formattingSyntax.value = ""
                    }
                }

                /*if (instanceData.software == "pleroma") {
                    binding.composePreviewButton.visibility = View.VISIBLE
                    reenableAttachments()
                }*/
            }

            viewModel.haveStickers.observe { haveStickers ->
                if (haveStickers) {
                    binding.composeStickerButton.visibility = View.VISIBLE
                }
            }

            viewModel.instanceStickers.observe { stickers ->
                if (stickers.isNotEmpty()) {
                    binding.composeStickerButton.visibility = View.VISIBLE
                    enableButton(
                        binding.composeStickerButton,
                        clickable = true,
                        colorActive = true
                    )
                    binding.stickerKeyboard.setupStickerKeyboard(this@ComposeActivity, stickers)
                }
            }

            viewModel.emoji.observe { emoji -> setEmojiList(emoji) }
            combineLiveData(
                viewModel.markMediaAsSensitive,
                viewModel.showContentWarning
            ) { markSensitive, showContentWarning ->
                updateSensitiveMediaToggle(markSensitive, showContentWarning)
                showContentWarning(showContentWarning)
            }.subscribe()

            viewModel.statusVisibility.observe { visibility ->
                setStatusVisibility(visibility)
            }

            viewModel.media.observe { media ->
                mediaAdapter.submitList(media)
                if (media.size != mediaCount) {
                    mediaCount = media.size
                    binding.composeMediaPreviewBar.visible(media.isNotEmpty())
                    updateSensitiveMediaToggle(
                        viewModel.markMediaAsSensitive.value != false,
                        viewModel.showContentWarning.value != false
                    )
                }
            }

            viewModel.poll.observe { poll ->
                binding.pollPreview.visible(poll != null)
                poll?.let(binding.pollPreview::setPoll)
            }

            viewModel.scheduledAt.observe { scheduledAt ->
                if (scheduledAt == null) {
                    binding.composeScheduleView.resetSchedule()
                } else {
                    binding.composeScheduleView.setDateTime(scheduledAt)
                }
                updateScheduleButton()
            }

            combineOptionalLiveData(viewModel.media, viewModel.poll) { media, poll ->
                if (!viewModel.hasNoAttachmentLimits) {
                    val active = poll == null && media!!.size != 4 &&
                        (media.isEmpty() || media.first().type == QueuedMedia.Type.IMAGE)
                    enableButton(binding.composeAddMediaButton, active, active)
                    enablePollButton(media.isNullOrEmpty())
                }
            }.subscribe()

            viewModel.uploadError.observe {
                displayTransientError(R.string.error_media_upload_sending)
            }

            viewModel.setupComplete.observe {
                // Focus may have changed during view model setup, ensure initial focus is on the edit field
                binding.composeEditField.requestFocus()
            }

            viewModel.formattingSyntax.observe {
                if (it.isEmpty()) {
                    enableFormattingSyntaxButton(suggestFormattingSyntax, false)
                    setIconForSyntax(suggestFormattingSyntax, false)
                } else {
                    val enable = it == suggestFormattingSyntax

                    enableFormattingSyntaxButton(it, enable)
                    setIconForSyntax(it, enable)
                }
            }

            viewModel.postExpiresIn.observe { postExpiresIn ->
                @ColorInt
                val color = ThemeUtils.getColor(
                    this@ComposeActivity,
                    if (postExpiresIn != null) {
                        R.attr.colorPrimary
                    } else {
                        android.R.attr.textColorTertiary
                    }
                )
                binding.postExpiresInButton.drawable.colorFilter =
                    PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
    }

    private fun setupButtons() {
        binding.composeOptionsBottomSheet.listener = this

        composeOptionsBehavior = BottomSheetBehavior.from(binding.composeOptionsBottomSheet)
        addMediaBehavior = BottomSheetBehavior.from(binding.addMediaBottomSheet)
        scheduleBehavior = BottomSheetBehavior.from(binding.composeScheduleView)
        emojiBehavior = BottomSheetBehavior.from(binding.emojiView)
        stickerBehavior = BottomSheetBehavior.from(binding.stickerKeyboard)
        previewBehavior = BottomSheetBehavior.from(binding.previewScroll)

        enableButton(binding.composeEmojiButton, clickable = false, colorActive = false)
        enableButton(binding.composeStickerButton, clickable = false, colorActive = false)

        // Setup the interface buttons.
        binding.composeTootButton.setOnClickListener { onSendClicked(false) }
        binding.composePreviewButton.setOnClickListener { onSendClicked(true) }
        binding.composeAddMediaButton.setOnClickListener { openPickDialog() }
        binding.composeToggleVisibilityButton.setOnClickListener { showComposeOptions() }
        binding.composeContentWarningButton.setOnClickListener { onContentWarningChanged() }
        binding.composeEmojiButton.setOnClickListener { showEmojis() }
        binding.composeHideMediaButton.setOnClickListener { toggleHideMedia() }
        binding.composeScheduleButton.setOnClickListener { onScheduleClick() }
        binding.composeScheduleView.setResetOnClickListener { resetSchedule() }
        binding.composeFormattingSyntax.setOnClickListener { toggleFormattingMode() }
        binding.composeFormattingSyntax.setOnLongClickListener { selectFormattingSyntax() }
        binding.postExpiresInButton.setOnClickListener { togglePostExpiresIn() }
        binding.postExpiresInButton.setOnLongClickListener { showInfoPostExpiresIn() }
        binding.composeStickerButton.setOnClickListener { showStickers() }
        binding.atButton.setOnClickListener { atButtonClicked() }
        binding.hashButton.setOnClickListener { hashButtonClicked() }
        binding.codeButton.setOnClickListener { codeButtonClicked() }
        binding.linkButton.setOnClickListener { linkButtonClicked() }
        binding.strikethroughButton.setOnClickListener { strikethroughButtonClicked() }
        binding.italicButton.setOnClickListener { italicButtonClicked() }
        binding.boldButton.setOnClickListener { boldButtonClicked() }

        val textColor = ThemeUtils.getColor(this, android.R.attr.textColorTertiary)

        val cameraIcon = IconicsDrawable(this, GoogleMaterial.Icon.gmd_camera_alt).apply {
            colorInt = textColor; sizeDp = 18
        }
        binding.actionPhotoTake.setCompoundDrawablesRelativeWithIntrinsicBounds(
            cameraIcon,
            null,
            null,
            null
        )

        val imageIcon = IconicsDrawable(this, GoogleMaterial.Icon.gmd_image).apply {
            colorInt = textColor; sizeDp = 18
        }
        binding.actionPhotoPick.setCompoundDrawablesRelativeWithIntrinsicBounds(
            imageIcon,
            null,
            null,
            null
        )

        val pollIcon = IconicsDrawable(this, GoogleMaterial.Icon.gmd_poll).apply {
            colorInt = textColor; sizeDp = 18
        }
        binding.addPollTextActionTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            pollIcon,
            null,
            null,
            null
        )

        binding.actionPhotoTake.setOnClickListener { initiateCameraApp() }
        binding.actionPhotoPick.setOnClickListener { onMediaPick() }
        binding.addPollTextActionTextView.setOnClickListener { openPollDialog() }
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.run {
            title = null
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_24dp)
        }
    }

    private fun setupAvatar(preferences: SharedPreferences, activeAccount: AccountEntity) {
        val actionBarSizeAttr = intArrayOf(R.attr.actionBarSize)
        val a = obtainStyledAttributes(null, actionBarSizeAttr)
        val avatarSize = a.getDimensionPixelSize(0, 1)
        a.recycle()

        val animateAvatars = preferences.getBoolean("animateGifAvatars", false)
        loadAvatar(
            activeAccount.profilePictureUrl,
            binding.composeAvatar,
            avatarSize / 8,
            animateAvatars
        )
        binding.composeAvatar.contentDescription = getString(
            R.string.compose_active_account_description,
            activeAccount.fullName
        )
    }

    private fun replaceTextAtCaret(text: CharSequence) {
        // If you select "backward" in an editable, you get SelectionStart > SelectionEnd
        val start =
            binding.composeEditField.selectionStart.coerceAtMost(binding.composeEditField.selectionEnd)
        val end =
            binding.composeEditField.selectionStart.coerceAtLeast(binding.composeEditField.selectionEnd)
        val textToInsert =
            if (start > 0 && !binding.composeEditField.text[start - 1].isWhitespace()) {
                " $text"
            } else {
                text
            }
        binding.composeEditField.text.replace(start, end, textToInsert)

        // Set the cursor after the inserted text
        binding.composeEditField.setSelection(start + text.length)
    }

    private fun enableFormattingSyntaxButton(syntax: String, enable: Boolean) {
        val stringId = when (syntax) {
            "text/html" -> R.string.action_html
            "text/bbcode" -> R.string.action_bbcode
            else -> R.string.action_markdown
        }

        val actionStringId =
            if (enable) R.string.action_disable_formatting_syntax else R.string.action_enable_formatting_syntax
        val tooltipText = getString(actionStringId).format(stringId)

        binding.composeFormattingSyntax.contentDescription = tooltipText

        @ColorInt val color = ThemeUtils.getColor(
            this,
            if (enable) R.attr.colorPrimary else android.R.attr.textColorTertiary
        )
        binding.composeFormattingSyntax.drawable.colorFilter =
            PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)

        enableMarkdownWYSIWYGButtons(enable)
    }

    private fun setIconForSyntax(syntax: String, enable: Boolean) {
        val drawableId = when (syntax) {
            "text/html" -> R.drawable.ic_html_24dp
            "text/bbcode" -> R.drawable.ic_bbcode_24dp
            else -> R.drawable.ic_markdown
        }

        suggestFormattingSyntax =
            if (drawableId == R.drawable.ic_markdown) "text/markdown" else syntax
        binding.composeFormattingSyntax.setImageResource(drawableId)
        enableFormattingSyntaxButton(syntax, enable)
    }

    private fun toggleFormattingMode() {
        if (viewModel.formattingSyntax.value!! == suggestFormattingSyntax) {
            viewModel.formattingSyntax.value = ""
        } else {
            viewModel.formattingSyntax.value = suggestFormattingSyntax
        }
    }

    private fun selectFormattingSyntax(): Boolean {
        val menu = PopupMenu(this, binding.composeFormattingSyntax)
        val plaintextId = 0
        val markdownId = 1
        val bbcodeId = 2
        val htmlId = 3

        menu.menu.add(0, plaintextId, 0, R.string.action_plaintext)
        if (viewModel.instanceParams.value?.postFormats?.contains(MARKDOWN) == true) {
            menu.menu.add(0, markdownId, 0, R.string.action_markdown)
        }

        if (viewModel.instanceParams.value?.postFormats?.contains(BBCODE) == true) {
            menu.menu.add(0, bbcodeId, 0, R.string.action_bbcode)
        }

        if (viewModel.instanceParams.value?.postFormats?.contains(HTML) == true) {
            menu.menu.add(0, htmlId, 0, R.string.action_html)
        }

        menu.setOnMenuItemClickListener { menuItem ->
            val choose = when (menuItem.itemId) {
                markdownId -> MARKDOWN.formatValue
                bbcodeId -> BBCODE.formatValue
                htmlId -> HTML.formatValue
                else -> ""
            }
            suggestFormattingSyntax = choose
            viewModel.formattingSyntax.value = choose
            true
        }
        menu.show()

        return true
    }

    private fun togglePostExpiresIn() {
        viewModel.postExpiresIn.value =
            if (viewModel.postExpiresIn.value != null || viewModel.postExpiresIn.value == 0) {
                null
            } else {
                accountManager.value.activeAccount!!.postExpiresIn
            }
    }

    private fun showInfoPostExpiresIn(): Boolean {
        AlertDialog.Builder(this)
                .setMessage(R.string.pref_default_post_expire_in_explanation)
                .setPositiveButton(android.R.string.ok, null)
                .show()

        return true
    }

    private fun enableMarkdownWYSIWYGButtons(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.codeButton.visibility = visibility
        binding.linkButton.visibility = visibility
        binding.strikethroughButton.visibility = visibility
        binding.italicButton.visibility = visibility
        binding.boldButton.visibility = visibility
    }

    fun prependSelectedWordsWith(text: CharSequence) {
        // If you select "backward" in an editable, you get SelectionStart > SelectionEnd
        val start =
            binding.composeEditField.selectionStart.coerceAtMost(binding.composeEditField.selectionEnd)
        val end =
            binding.composeEditField.selectionStart.coerceAtLeast(binding.composeEditField.selectionEnd)
        val editorText = binding.composeEditField.text

        if (start == end) {
            // No selection, just insert text at caret
            editorText.insert(start, text)
            // Set the cursor after the inserted text
            binding.composeEditField.setSelection(start + text.length)
        } else {
            var wasWord: Boolean
            var isWord = end < editorText.length && !Character.isWhitespace(editorText[end])
            var newEnd = end

            // Iterate the selection backward so we don't have to juggle indices on insertion
            var index = end - 1
            while (index >= start - 1 && index >= 0) {
                wasWord = isWord
                isWord = !Character.isWhitespace(editorText[index])
                if (wasWord && !isWord) {
                    // We've reached the beginning of a word, perform insert
                    editorText.insert(index + 1, text)
                    newEnd += text.length
                }
                --index
            }

            if (start == 0 && isWord) {
                // Special case when the selection includes the start of the text
                editorText.insert(0, text)
                newEnd += text.length
            }

            // Keep the same text (including insertions) selected
            binding.composeEditField.setSelection(start, newEnd)
        }
    }

    private fun atButtonClicked() {
        prependSelectedWordsWith("@")
    }

    private fun hashButtonClicked() {
        prependSelectedWordsWith("#")
    }

    private fun codeButtonClicked() {
        when (viewModel.formattingSyntax.value!!) {
            "text/markdown" -> MarkdownEdit.addCode(binding.composeEditField)
            "text/bbcode" -> BBCodeEdit.addCode(binding.composeEditField)
            "text/html" -> HTMLEdit.addCode(binding.composeEditField)
        }
    }

    private fun linkButtonClicked() {
        when (viewModel.formattingSyntax.value!!) {
            "text/markdown" -> MarkdownEdit.addLink(binding.composeEditField)
            "text/bbcode" -> BBCodeEdit.addLink(binding.composeEditField)
            "text/html" -> HTMLEdit.addLink(binding.composeEditField)
        }
    }

    private fun strikethroughButtonClicked() {
        when (viewModel.formattingSyntax.value!!) {
            "text/markdown" -> MarkdownEdit.addStrikeThrough(binding.composeEditField)
            "text/bbcode" -> BBCodeEdit.addStrikeThrough(binding.composeEditField)
            "text/html" -> HTMLEdit.addStrikeThrough(binding.composeEditField)
        }
    }

    private fun italicButtonClicked() {
        when (viewModel.formattingSyntax.value!!) {
            "text/markdown" -> MarkdownEdit.addItalic(binding.composeEditField)
            "text/bbcode" -> BBCodeEdit.addItalic(binding.composeEditField)
            "text/html" -> HTMLEdit.addItalic(binding.composeEditField)
        }
    }

    private fun boldButtonClicked() {
        when (viewModel.formattingSyntax.value!!) {
            "text/markdown" -> MarkdownEdit.addBold(binding.composeEditField)
            "text/bbcode" -> BBCodeEdit.addBold(binding.composeEditField)
            "text/html" -> HTMLEdit.addBold(binding.composeEditField)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(PHOTO_UPLOAD_URI_KEY, photoUploadUri)
        super.onSaveInstanceState(outState)
    }

    private fun displayTransientError(@StringRes stringId: Int) {
        val bar = Snackbar.make(binding.root, stringId, Snackbar.LENGTH_LONG)
        // necessary so snackbar is shown over everything
        bar.view.elevation = resources.getDimension(R.dimen.compose_activity_snackbar_elevation)
        bar.show()
    }

    private fun displayTransientError(message: String) {
        val bar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        // necessary so snackbar is shown over everything
        bar.view.elevation = resources.getDimension(R.dimen.compose_activity_snackbar_elevation)
        bar.show()
    }

    private fun toggleHideMedia() {
        this.viewModel.toggleMarkSensitive()
    }

    private fun updateSensitiveMediaToggle(
        markMediaSensitive: Boolean,
        contentWarningShown: Boolean
    ) {
        if (viewModel.media.value.isNullOrEmpty()) {
            binding.composeHideMediaButton.hide()
        } else {
            binding.composeHideMediaButton.show()
            @ColorInt val color = if (contentWarningShown) {
                binding.composeHideMediaButton.setImageResource(R.drawable.ic_hide_media_24dp)
                binding.composeHideMediaButton.isClickable = false
                ContextCompat.getColor(this, R.color.transparent_tusky_blue)
            } else {
                binding.composeHideMediaButton.isClickable = true
                if (markMediaSensitive) {
                    binding.composeHideMediaButton.setImageResource(R.drawable.ic_hide_media_24dp)
                    ContextCompat.getColor(this, R.color.tusky_blue)
                } else {
                    binding.composeHideMediaButton.setImageResource(R.drawable.ic_eye_24dp)
                    ThemeUtils.getColor(this, android.R.attr.textColorTertiary)
                }
            }
            binding.composeHideMediaButton.drawable.colorFilter =
                PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun updateScheduleButton() {
        @ColorInt val color = if (binding.composeScheduleView.time == null) {
            ThemeUtils.getColor(this, android.R.attr.textColorTertiary)
        } else {
            ContextCompat.getColor(this, R.color.tusky_blue)
        }
        binding.composeScheduleButton.drawable.colorFilter =
            PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private fun enableButtons(enable: Boolean) {
        binding.composeAddMediaButton.isClickable = enable
        binding.composeToggleVisibilityButton.isClickable = enable
        binding.composeEmojiButton.isClickable = enable
        binding.composeHideMediaButton.isClickable = enable
        binding.composeScheduleButton.isClickable = enable
        binding.composeFormattingSyntax.isClickable = enable
        binding.composeTootButton.isEnabled = enable
        binding.composePreviewButton.isEnabled = enable
        binding.composeStickerButton.isEnabled = enable
    }

    private fun setStatusVisibility(visibility: Status.Visibility) {
        binding.composeOptionsBottomSheet.setStatusVisibility(visibility)
        binding.composeTootButton.setStatusVisibility(visibility)

        val iconRes = when (visibility) {
            Status.Visibility.PUBLIC -> R.drawable.ic_public_24dp
            Status.Visibility.PRIVATE -> R.drawable.ic_lock_outline_24dp
            Status.Visibility.DIRECT -> R.drawable.ic_email_24dp
            Status.Visibility.UNLISTED -> R.drawable.ic_lock_open_24dp
            Status.Visibility.LOCAL -> R.drawable.ic_local_24dp
            else -> R.drawable.ic_lock_open_24dp
        }
        binding.composeToggleVisibilityButton.setImageResource(iconRes)
    }

    private fun showComposeOptions() {
        if (composeOptionsBehavior.state == BottomSheetBehavior.STATE_HIDDEN || composeOptionsBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            composeOptionsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            scheduleBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            previewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            composeOptionsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun onScheduleClick() {
        if (viewModel.scheduledAt.value == null) {
            binding.composeScheduleView.openPickDateDialog()
        } else {
            showScheduleView()
        }
    }

    private fun showScheduleView() {
        if (scheduleBehavior.state == BottomSheetBehavior.STATE_HIDDEN || scheduleBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            scheduleBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            composeOptionsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            previewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            scheduleBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun showEmojis() {
        binding.emojiView.adapter?.let {
            if (it.itemCount == 0) {
                val errorMessage = getString(
                    R.string.error_no_custom_emojis,
                    accountManager.value.activeAccount!!.domain
                )
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            } else {
                if (emojiBehavior.state == BottomSheetBehavior.STATE_HIDDEN || emojiBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    emojiBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    composeOptionsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    scheduleBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    previewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                } else {
                    emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }
    }

    private fun openPickDialog() {
        if (addMediaBehavior.state == BottomSheetBehavior.STATE_HIDDEN || addMediaBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            addMediaBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            composeOptionsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            scheduleBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            previewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun onMediaPick() {
        addMediaBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Wait until bottom sheet is not collapsed and show next screen after
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    val askForPermissions = if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                        )
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

                    val permissions = askForPermissions.mapNotNull { permission ->
                        permission.takeIf {
                            ContextCompat.checkSelfPermission(this@ComposeActivity, it) ==
                                PackageManager.PERMISSION_DENIED
                        }
                    }.toTypedArray()

                    addMediaBehavior.removeBottomSheetCallback(this)

                    if (permissions.isNotEmpty()) {
                        ActivityCompat.requestPermissions(
                            this@ComposeActivity,
                            permissions,
                            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                        )
                    } else {
                        initiateMediaPicking()
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        addMediaBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun openPollDialog() {
        addMediaBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        val instanceParams = viewModel.instanceParams.value!!
        showAddPollDialog(
            this,
            viewModel.poll.value,
            instanceParams.pollMaxOptions,
            instanceParams.pollMaxLength,
            viewModel::updatePoll
        )
    }

    private fun setupPollView() {
        val margin = resources.getDimensionPixelSize(R.dimen.compose_media_preview_margin)
        val marginBottom =
            resources.getDimensionPixelSize(R.dimen.compose_media_preview_margin_bottom)

        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(margin, margin, margin, marginBottom)
        binding.pollPreview.layoutParams = layoutParams

        binding.pollPreview.setOnClickListener {
            val popup = PopupMenu(this, binding.pollPreview)
            val editId = 1
            val removeId = 2
            popup.menu.add(0, editId, 0, R.string.edit_poll)
            popup.menu.add(0, removeId, 0, R.string.action_remove)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    editId -> openPollDialog()
                    removeId -> removePoll()
                }
                true
            }
            popup.show()
        }
    }

    private fun removePoll() {
        viewModel.poll.value = null
        binding.pollPreview.hide()
    }

    override fun onVisibilityChanged(visibility: Status.Visibility) {
        composeOptionsBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        viewModel.statusVisibility.value = visibility
    }

    @VisibleForTesting
    fun calculateTextLength(): Int {
        var offset = 0
        val urlSpans = binding.composeEditField.urls
        if (urlSpans != null) {
            for (span in urlSpans) {
                offset += max(0, span.url.length - MAXIMUM_URL_LENGTH)
            }
        }
        var length = binding.composeEditField.length() - offset
        if (viewModel.showContentWarning.value!!) {
            length += binding.composeContentWarningField.length()
        }
        return length
    }

    private fun updateVisibleCharactersLeft() {
        val remainingLength = maximumTootCharacters - calculateTextLength()
        binding.composeCharactersLeftView.text =
            String.format(Locale.getDefault(), "%d", remainingLength)

        val textColor = if (remainingLength < 0) {
            ContextCompat.getColor(this, R.color.tusky_red)
        } else {
            ThemeUtils.getColor(this, android.R.attr.textColorTertiary)
        }
        binding.composeCharactersLeftView.setTextColor(textColor)
    }

    private fun onContentWarningChanged() {
        val showWarning = binding.composeContentWarningBar.isGone
        viewModel.contentWarningChanged(showWarning)
        updateVisibleCharactersLeft()
    }

    private fun verifyScheduledTime(): Boolean {
        return binding.composeScheduleView.verifyScheduledTime(
            binding.composeScheduleView.getDateTime(
                viewModel.scheduledAt.value
            )
        )
    }

    private fun onSendClicked(preview: Boolean) {
        if (preview && previewBehavior.state != BottomSheetBehavior.STATE_HIDDEN &&
            previewBehavior.state != BottomSheetBehavior.STATE_COLLAPSED
        ) {
            previewBehavior.state = BottomSheetBehavior.STATE_HIDDEN

            return
        }

        if (verifyScheduledTime()) {
            sendStatus(preview)
        } else {
            showScheduleView()
        }
    }

    private fun onStatusPreviewReady(status: Status) {
        enableButtons(true)
        if (previewBehavior.state != BottomSheetBehavior.STATE_HIDDEN &&
            previewBehavior.state != BottomSheetBehavior.STATE_COLLAPSED
        ) {
            previewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            binding.previewView.setupWithStatus(status)
            previewBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            composeOptionsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            scheduleBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    /** This is for the fancy keyboards which can insert images and stuff. */
    override fun onCommitContent(
        inputContentInfo: InputContentInfoCompat,
        flags: Int,
        opts: Bundle?
    ): Boolean {
        // Verify the returned content's type is of the correct MIME type
        val supported = inputContentInfo.description.hasMimeType("image/*")

        if (supported) {
            val lacksPermission =
                (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
            if (lacksPermission) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    Timber.e("InputContentInfoCompat#requestPermission() failed: ${e.message}")
                    return false
                }
            }
            pickMedia(inputContentInfo.contentUri, inputContentInfo)
            return true
        }

        return false
    }

    private fun sendStatus(preview: Boolean) {
        enableButtons(false)
        val tempText = binding.composeEditField.text.toString()
        val contentText = if (viewModel.composeWithZwsp.value == true) {
            tempText.composeWithZwsp()
        } else {
            tempText
        }

        var spoilerText = ""
        if (viewModel.showContentWarning.value!!) {
            spoilerText = binding.composeContentWarningField.text.toString()
        }
        val characterCount = calculateTextLength()
        if ((characterCount <= 0 || contentText.isBlank()) && viewModel.media.value!!.isEmpty()) {
            binding.composeEditField.error = getString(R.string.error_empty)
            enableButtons(true)
        } else if (characterCount <= maximumTootCharacters) {
            if (viewModel.media.value!!.isNotEmpty()) {
                finishingUploadDialog = ProgressDialog.show(
                    this,
                    getString(R.string.dialog_title_finishing_media_upload),
                    getString(R.string.dialog_message_uploading_media),
                    true,
                    true
                )
            }

            viewModel.sendStatus(contentText, spoilerText, preview).observeOnce(this) {
                finishingUploadDialog?.dismiss()
                if (!preview) {
                    deleteDraftAndFinish()
                }
            }
        } else {
            binding.composeEditField.error = getString(R.string.error_compose_character_limit)
            enableButtons(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateMediaPicking()
            } else {
                val bar = Snackbar.make(
                    binding.root,
                    R.string.error_media_upload_permission,
                    Snackbar.LENGTH_SHORT
                ).apply {
                    setAction(R.string.action_retry) { onMediaPick() }
                    view.elevation =
                        resources.getDimension(R.dimen.compose_activity_snackbar_elevation)
                }.show()
            }
        }
    }

    private fun initiateCameraApp() {
        addMediaBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // We don't need to ask for permission in this case, because the used calls require
        // android.permission.WRITE_EXTERNAL_STORAGE only on SDKs *older* than Kitkat, which was
        // way before permission dialogues have been introduced.
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File = try {
                createNewImageFile(this)
            } catch (ex: IOException) {
                displayTransientError(R.string.error_media_upload_opening)
                return
            }

            // Continue only if the File was successfully created
            photoUploadUri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUploadUri)
            startActivityForResult(intent, MEDIA_TAKE_PHOTO_RESULT)
        }
    }

    private fun initiateMediaPicking() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)

        if (!viewModel.hasNoAttachmentLimits) {
            val mimeTypes = arrayOf("image/*", "video/*", "audio/*")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, MEDIA_PICK_RESULT)
    }

    private fun enableButton(button: ImageButton, clickable: Boolean, colorActive: Boolean) {
        button.isEnabled = clickable
        ThemeUtils.setDrawableTint(
            this,
            button.drawable,
            if (colorActive) {
                android.R.attr.textColorTertiary
            } else {
                R.attr.textColorDisabled
            }
        )
    }

    private fun enablePollButton(enable: Boolean) {
        binding.addPollTextActionTextView.isEnabled = enable
        val textColor = ThemeUtils.getColor(
            this,
            if (enable) {
                android.R.attr.textColorTertiary
            } else {
                R.attr.textColorDisabled
            }
        )
        binding.addPollTextActionTextView.setTextColor(textColor)
        binding.addPollTextActionTextView.compoundDrawablesRelative[0].colorFilter =
            PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN)
    }

    private fun removeMediaFromQueue(item: QueuedMedia) {
        viewModel.removeMediaFromQueue(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK && requestCode == MEDIA_PICK_RESULT && intent != null) {
            if (intent.data != null) {
                Timber.d("Upload a single media item")

                pickMedia(intent.data!!)
            } else if (intent.clipData != null) {
                val clipData = intent.clipData!!
                val count = clipData.itemCount

                viewModel.instanceParams.value?.maxMediaAttachments?.let { maxMediaAttachments ->
                    Timber.d("Max media attachments upload [$maxMediaAttachments]")

                    if (maxMediaAttachments > 0 && (mediaCount + count > maxMediaAttachments)) {
                        Toast.makeText(
                            this,
                            getString(
                                R.string.error_upload_max_media_reached,
                                maxMediaAttachments
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        for (i in 0 until count) {
                            val imageUri = clipData.getItemAt(i).uri
                            pickMedia(imageUri)
                        }
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == MEDIA_TAKE_PHOTO_RESULT) {
            pickMedia(photoUploadUri!!)
        }
    }

    private fun pickMedia(
        uri: Uri,
        contentInfoCompat: InputContentInfoCompat? = null,
        filename: String? = null
    ) {
        withLifecycleContext {
            viewModel.pickMedia(uri, filename ?: uri.toFileName(contentResolver))
                .observe { exceptionOrItem ->
                    contentInfoCompat?.releasePermission()

                    exceptionOrItem.asLeftOrNull()?.let {
                        val errorId = when (it) {
                            is VideoSizeException -> {
                                String.format(
                                    getString(R.string.error_video_upload_size),
                                    it.size.toMB().toString()
                                )
                            }
                            is AudioSizeException -> {
                                String.format(
                                    getString(R.string.error_audio_upload_size),
                                    it.size.toMB().toString()
                                )
                            }
                            is ImageSizeException -> {
                                String.format(
                                    getString(R.string.error_image_upload_size),
                                    it.size.toMB().toString()
                                )
                            }
                            is MediaSizeException -> {
                                "${getString(R.string.error_media_upload_size)}: ${it.size.toMB()}"
                            }
                            is VideoOrImageException -> {
                                getString(R.string.error_media_upload_image_or_video)
                            }
                            else -> {
                                Timber.d("That file could not be opened: $it")
                                getString(R.string.error_media_upload_opening)
                            }
                        }
                        displayTransientError(errorId)
                    }
                }
        }
    }

    private fun showContentWarning(show: Boolean) {
        TransitionManager.beginDelayedTransition(binding.composeContentWarningBar.parent as ViewGroup)
        @ColorInt val color = if (show) {
            binding.composeContentWarningBar.show()
            binding.composeContentWarningField.setSelection(binding.composeContentWarningField.text.length)
            binding.composeContentWarningField.requestFocus()
            ContextCompat.getColor(this, R.color.tusky_blue)
        } else {
            binding.composeContentWarningBar.hide()
            binding.composeEditField.requestFocus()
            ThemeUtils.getColor(this, android.R.attr.textColorTertiary)
        }
        binding.composeContentWarningButton.drawable.colorFilter =
            PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            handleCloseButton()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Timber.d(event.toString())

        if (event.action == KeyEvent.ACTION_UP) {
            if (event.isCtrlPressed) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    // Send toot by pressing CTRL + ENTER
                    this.onSendClicked(false)
                    return true
                }
            }

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleCloseButton() {
        val contentText = binding.composeEditField.text.toString()
        val contentWarning = binding.composeContentWarningField.text.toString()
        if (viewModel.didChange(contentText, contentWarning)) {
            AlertDialog.Builder(this)
                .setMessage(R.string.compose_save_draft)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    saveDraftAndFinish(contentText, contentWarning)
                }
                .setNegativeButton(R.string.action_delete) { _, _ -> deleteDraftAndFinish() }
                .show()
        } else {
            finishWithoutSlideOutAnimation()
        }
    }

    private fun deleteDraftAndFinish() {
        viewModel.deleteDraft()
        finishWithoutSlideOutAnimation()
    }

    private fun saveDraftAndFinish(contentText: String, contentWarning: String) {
        viewModel.saveDraft(contentText, contentWarning)
        finishWithoutSlideOutAnimation()
    }

    override fun search(token: String): List<ComposeAutoCompleteAdapter.AutocompleteResult> {
        return viewModel.searchAutocompleteSuggestions(token)
    }

    override fun onEmojiSelected(shortcode: String) {
        replaceTextAtCaret(":$shortcode: ")
    }

    private fun setEmojiList(emojiList: List<Emoji>?) {
        if (emojiList != null) {
            binding.emojiView.adapter = EmojiAdapter(
                emojiList,
                this@ComposeActivity,
                preferences.getBoolean(PrefKeys.ANIMATE_CUSTOM_EMOJIS, false)
            )
            enableButton(binding.composeEmojiButton, true, emojiList.isNotEmpty())
        }
    }

    private fun showStickers() {
        if (stickerBehavior.state == BottomSheetBehavior.STATE_HIDDEN || stickerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            stickerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            composeOptionsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            scheduleBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            previewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun onEmojiSelected(id: String, shortcode: String) {
        // pickMedia(Uri.parse(shbindingortcode))

        Glide.with(this).asFile().load(shortcode).into(object : CustomTarget<File>() {
            override fun onLoadCleared(placeholder: Drawable?) {
                displayTransientError(R.string.error_sticker_fetch)
            }

            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                val cut = shortcode.lastIndexOf('/')
                val filename = if (cut != -1) shortcode.substring(cut + 1) else "unknown.png"
                pickMedia(resource.toUri(), null, filename)
            }
        })
        stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    data class QueuedMedia(
        val localId: Long,
        val uri: Uri,
        val type: Int,
        val mediaSize: Long,
        val originalFileName: String,
        val noChanges: Boolean = false,
        val anonymizeFileName: Boolean = false,
        val uploadPercent: Int = 0,
        val id: String? = null,
        val description: String? = null
    ) {
        companion object Type {
            const val IMAGE: Int = 0
            const val VIDEO: Int = 1
            const val AUDIO: Int = 2
            const val UNKNOWN: Int = 3
        }
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        binding.composeScheduleView.onTimeSet(hourOfDay, minute)
        viewModel.updateScheduledAt(binding.composeScheduleView.time)
        if (verifyScheduledTime()) {
            scheduleBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            showScheduleView()
        }
    }

    private fun resetSchedule() {
        viewModel.updateScheduledAt(null)
        scheduleBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    @Parcelize
    data class ComposeOptions(
        // Let's keep fields var until all consumers are Kotlin
        var scheduledTootId: String? = null,
        var savedTootUid: Int? = null,
        var draftId: Int? = null,
        var tootText: String? = null,
        var mediaUrls: List<String>? = null,
        var mediaDescriptions: List<String>? = null,
        var mentionedUsernames: Set<String>? = null,
        var inReplyToId: String? = null,
        var replyVisibility: Status.Visibility? = null,
        var visibility: Status.Visibility? = null,
        var contentWarning: String? = null,
        var replyingStatusAuthor: String? = null,
        var replyingStatusContent: String? = null,
        var mediaAttachments: List<Attachment>? = null,
        var draftAttachments: List<DraftAttachment>? = null,
        var scheduledAt: String? = null,
        var expiresIn: Int = 0,
        var sensitive: Boolean? = null,
        var poll: NewPoll? = null,
        var formattingSyntax: String? = null,
        var modifiedInitialState: Boolean? = null,
        var quotePostId: String? = null
    ) : Parcelable

    companion object {
        private const val MEDIA_PICK_RESULT = 1
        private const val MEDIA_TAKE_PHOTO_RESULT = 2
        private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1

        internal const val COMPOSE_OPTIONS_EXTRA = "COMPOSE_OPTIONS"
        private const val PHOTO_UPLOAD_URI_KEY = "PHOTO_UPLOAD_URI"

        // Mastodon only counts URLs as this long in terms of status character limits
        @VisibleForTesting
        const val MAXIMUM_URL_LENGTH = 23

        @JvmStatic
        fun startIntent(context: Context, options: ComposeOptions): Intent {
            return Intent(context, ComposeActivity::class.java).apply {
                putExtra(COMPOSE_OPTIONS_EXTRA, options)
            }
        }

        fun canHandleMimeType(mimeType: String?): Boolean {
            return mimeType != null && (
                mimeType.startsWith("image/") || mimeType.startsWith("video/") || mimeType.startsWith(
                    "audio/"
                ) || mimeType == "text/plain"
                )
        }
    }
}
