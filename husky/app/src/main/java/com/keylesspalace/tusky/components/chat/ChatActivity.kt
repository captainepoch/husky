/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2020  Alibek Omarov
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

package com.keylesspalace.tusky.components.chat

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.arch.core.util.Function
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListUpdateCallback
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.keylesspalace.tusky.BottomSheetActivity
import com.keylesspalace.tusky.BuildConfig
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.ViewMediaActivity
import com.keylesspalace.tusky.ViewTagActivity
import com.keylesspalace.tusky.adapter.ChatMessagesAdapter
import com.keylesspalace.tusky.adapter.ChatMessagesViewHolder
import com.keylesspalace.tusky.adapter.EmojiAdapter
import com.keylesspalace.tusky.adapter.OnEmojiSelectedListener
import com.keylesspalace.tusky.adapter.TimelineAdapter
import com.keylesspalace.tusky.appstore.ChatMessageDeliveredEvent
import com.keylesspalace.tusky.appstore.ChatMessageReceivedEvent
import com.keylesspalace.tusky.appstore.Event
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.components.common.AudioSizeException
import com.keylesspalace.tusky.components.common.MediaSizeException
import com.keylesspalace.tusky.components.common.VideoOrImageException
import com.keylesspalace.tusky.components.common.VideoSizeException
import com.keylesspalace.tusky.components.common.createNewImageFile
import com.keylesspalace.tusky.components.common.toFileName
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.components.compose.ComposeAutoCompleteAdapter
import com.keylesspalace.tusky.components.compose.dialog.makeCaptionDialog
import com.keylesspalace.tusky.core.extensions.afterTextChanged
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.core.utils.InstanceConstants
import com.keylesspalace.tusky.databinding.ActivityChatBinding
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.entity.Chat
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.interfaces.ChatActionListener
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.repository.ChatMesssageOrPlaceholder
import com.keylesspalace.tusky.repository.ChatRepository
import com.keylesspalace.tusky.repository.Placeholder
import com.keylesspalace.tusky.repository.TimelineRequestMode
import com.keylesspalace.tusky.service.MessageToSend
import com.keylesspalace.tusky.service.ServiceClient
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.ComposeTokenizer
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.util.PairedList
import com.keylesspalace.tusky.util.ThemeUtils
import com.keylesspalace.tusky.util.ViewDataUtils
import com.keylesspalace.tusky.util.dec
import com.keylesspalace.tusky.util.emojify
import com.keylesspalace.tusky.util.highlightSpans
import com.keylesspalace.tusky.util.inc
import com.keylesspalace.tusky.util.loadAvatar
import com.keylesspalace.tusky.util.withLifecycleContext
import com.keylesspalace.tusky.view.EmojiKeyboard
import com.keylesspalace.tusky.viewdata.ChatMessageViewData
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.uber.autodispose.android.lifecycle.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatActivity :
    BottomSheetActivity(),
    ChatActionListener,
    ComposeAutoCompleteAdapter.AutocompletionProvider,
    EmojiKeyboard.OnEmojiSelectedListener,
    OnEmojiSelectedListener,
    InputConnectionCompat.OnCommitContentListener {

    private val binding by viewBinding(ActivityChatBinding::inflate)

    private val LOAD_AT_ONCE = 30
    private val eventHub: EventHub by inject()
    private val api: MastodonApi by inject()
    private val chatsRepo: ChatRepository by inject()
    private val serviceClient: ServiceClient by inject()
    val viewModel: ChatViewModel by viewModel()
    private lateinit var adapter: ChatMessagesAdapter

    @VisibleForTesting
    var maximumTootCharacters = InstanceConstants.DEFAULT_CHARACTER_LIMIT

    private val msgs =
        PairedList<ChatMesssageOrPlaceholder, ChatMessageViewData?>(
            Function<ChatMesssageOrPlaceholder, ChatMessageViewData?> { input ->
                input.asRightOrNull()?.let(ViewDataUtils::chatMessageToViewData)
                    ?: ChatMessageViewData.Placeholder(input.asLeft().id, false)
            }
        )

    private var bottomLoading = false
    private var isNeedRefresh = false
    private var didLoadEverythingBottom = false
    private var initialUpdateFailed = false
    private var haveStickers = false

    private lateinit var addMediaBehavior: BottomSheetBehavior<*>
    private lateinit var emojiBehavior: BottomSheetBehavior<*>
    private lateinit var stickerBehavior: BottomSheetBehavior<*>

    private var finishingUploadDialog: ProgressDialog? = null
    private var photoUploadUri: Uri? = null

    private enum class FetchEnd {
        TOP, BOTTOM, MIDDLE
    }

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            Timber.d("onInserted")
            adapter.notifyItemRangeInserted(position, count)
            if (position == 0) {
                binding.recycler.scrollToPosition(0)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            Timber.d("onRemoved")
            adapter.notifyItemRangeRemoved(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            Timber.d("onMoved")
            adapter.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            Timber.d("onChanged")
            adapter.notifyItemRangeChanged(position, count, payload)
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<ChatMessageViewData>() {
        override fun areItemsTheSame(
            oldItem: ChatMessageViewData,
            newItem: ChatMessageViewData
        ): Boolean {
            return oldItem.getViewDataId() == newItem.getViewDataId()
        }

        override fun areContentsTheSame(
            oldItem: ChatMessageViewData,
            newItem: ChatMessageViewData
        ): Boolean {
            // Items are different always. It allows to refresh timestamp on every view
            // holder update
            return false
        }

        override fun getChangePayload(
            oldItem: ChatMessageViewData,
            newItem: ChatMessageViewData
        ): Any? {
            return if (oldItem.deepEquals(newItem)) {
                // If items are equal - update timestamp only
                listOf(ChatMessagesViewHolder.Key.KEY_CREATED)
            } else {
                // If items are different - update a whole view holder
                null
            }
        }
    }

    private val differ = AsyncListDiffer(
        listUpdateCallback,
        AsyncDifferConfig.Builder(diffCallback).build()
    )

    private val dataSource = object : TimelineAdapter.AdapterDataSource<ChatMessageViewData> {
        override fun getItemCount(): Int {
            return differ.currentList.size
        }

        override fun getItemAt(pos: Int): ChatMessageViewData {
            return differ.currentList[pos]
        }
    }

    private lateinit var chatId: String
    private lateinit var avatarUrl: String
    private lateinit var displayName: String
    private lateinit var username: String
    private lateinit var emojis: ArrayList<Emoji>

    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (accountManager.value.activeAccount == null) {
            throw Exception("No active account!")
        }

        chatId = intent.getStringExtra(ID)
            ?: throw IllegalArgumentException("Can't open ChatActivity without chatId")
        avatarUrl = intent.getStringExtra(AVATAR_URL)
            ?: throw IllegalArgumentException("Can't open ChatActivity without avatarUrl")
        displayName = intent.getStringExtra(DISPLAY_NAME)
            ?: throw IllegalArgumentException("Can't open ChatActivity without displayName")
        username = intent.getStringExtra(USERNAME)
            ?: throw IllegalArgumentException("Can't open ChatActivity without username")
        emojis = intent.getParcelableArrayListExtra<Emoji>(EMOJIS)
            ?: throw IllegalArgumentException("Can't open ChatActivity without emojis")

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        subscribeToUpdates()

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        viewModel.tryFetchStickers = preferences.getBoolean(PrefKeys.STICKERS, false)
        viewModel.anonymizeNames = preferences.getBoolean(PrefKeys.ANONYMIZE_FILENAMES, false)

        setupHeader()
        setupChat()
        setupAttachment()
        setupComposeField(savedInstanceState?.getString(MESSAGE_KEY))
        setupButtons()

        viewModel.setup()

        photoUploadUri = savedInstanceState?.getParcelable(PHOTO_UPLOAD_URI_KEY)

        eventHub.events
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe { event: Event? ->
                when (event) {
                    is ChatMessageDeliveredEvent -> {
                        if (event.chatMsg.chatId == chatId) {
                            onRefresh()
                            enableButton(binding.attachmentButton, true, true)
                            enableButton(binding.stickerButton, haveStickers, haveStickers)

                            sending = false
                            enableSendButton()
                        }
                    }
                    is ChatMessageReceivedEvent -> {
                        if (event.chatMsg.chatId == chatId) {
                            onRefresh()
                        }
                    }
                }
            }

        tryCache()
    }

    private fun setupHeader() {
        supportActionBar?.run {
            title = ""
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        loadAvatar(
            avatarUrl,
            binding.chatAvatar,
            resources.getDimensionPixelSize(R.dimen.avatar_radius_24dp),
            true
        )
        binding.chatTitle.text = displayName.emojify(emojis, binding.chatTitle, true)
        binding.chatUsername.text = username
    }

    private fun setupChat() {
        adapter =
            ChatMessagesAdapter(dataSource, this, accountManager.value.activeAccount!!.accountId)

        // TODO: a11y
        binding.recycler.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        binding.recycler.layoutManager = layoutManager
        // recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.recycler.adapter = adapter
    }

    private fun setupAttachment() {
        val onMediaPick = View.OnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            val addCaptionId = 1
            val removeId = 2
            popup.menu.add(0, addCaptionId, 0, R.string.action_set_caption)
            popup.menu.add(0, removeId, 0, R.string.action_remove)
            popup.setOnMenuItemClickListener { menuItem ->
                viewModel.media.value?.get(0)?.let {
                    when (menuItem.itemId) {
                        addCaptionId -> {
                            makeCaptionDialog(it.description, it.uri) { newDescription ->
                                viewModel.updateDescription(it.localId, newDescription)
                            }
                        }
                        removeId -> {
                            viewModel.removeMediaFromQueue(it)
                        }
                    }
                }
                true
            }
            popup.show()
        }

        binding.imageAttachment.setOnClickListener(onMediaPick)
        binding.textAttachment.setOnClickListener(onMediaPick)
    }

    private fun setupComposeField(startingText: String?) {
        binding.editText.setOnCommitContentListener(this)

        binding.editText.setOnKeyListener { _, keyCode, event -> this.onKeyDown(keyCode, event) }

        binding.editText.setAdapter(
            ComposeAutoCompleteAdapter(this)
        )
        binding.editText.setTokenizer(ComposeTokenizer())

        binding.editText.setText(startingText)
        binding.editText.setSelection(binding.editText.length())

        val mentionColour = binding.editText.linkTextColors.defaultColor
        highlightSpans(binding.editText.text, mentionColour)
        binding.editText.afterTextChanged { editable ->
            highlightSpans(editable, mentionColour)
            enableSendButton()
        }

        // work around Android platform bug -> https://issuetracker.google.com/issues/67102093
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1
        ) {
            binding.editText.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    private var sending = false
    private fun enableSendButton() {
        if (sending) {
            return
        }

        val haveMedia = viewModel.media.value?.isNotEmpty() ?: false
        val haveText = binding.editText.text.isNotEmpty()

        enableButton(binding.sendButton, haveMedia || haveText, haveMedia || haveText)
    }

    override fun search(token: String): List<ComposeAutoCompleteAdapter.AutocompleteResult> {
        return viewModel.searchAutocompleteSuggestions(token)
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

    private fun subscribeToUpdates() {
        withLifecycleContext {
            viewModel.instanceParams.observe { instanceData ->
                maximumTootCharacters = instanceData.chatLimit
            }
            viewModel.instanceStickers.observe { stickers ->
                if (stickers.isNotEmpty()) {
                    haveStickers = true
                    binding.stickerButton.visibility = View.VISIBLE
                    enableButton(binding.stickerButton, true, true)
                    binding.stickerKeyboard.setupStickerKeyboard(this@ChatActivity, stickers)
                }
            }
            viewModel.emoji.observe { setEmojiList(it) }
            viewModel.media.observe {
                val notHaveMedia = it.isEmpty()

                enableSendButton()
                enableButton(binding.attachmentButton, notHaveMedia, notHaveMedia)
                enableButton(
                    binding.stickerButton,
                    haveStickers && notHaveMedia,
                    haveStickers && notHaveMedia
                )

                if (!notHaveMedia) {
                    val media = it[0]

                    when (media.type) {
                        ComposeActivity.QueuedMedia.UNKNOWN -> {
                            binding.textAttachment.visibility = View.VISIBLE
                            binding.imageAttachment.visibility = View.GONE

                            binding.textAttachment.text = media.originalFileName
                            binding.textAttachment.setChecked(!media.description.isNullOrEmpty())
                            binding.textAttachment.setProgress(media.uploadPercent)
                        }
                        ComposeActivity.QueuedMedia.AUDIO -> {
                            binding.imageAttachment.visibility = View.VISIBLE
                            binding.textAttachment.visibility = View.GONE

                            binding.imageAttachment.setChecked(!media.description.isNullOrEmpty())
                            binding.imageAttachment.setProgress(media.uploadPercent)
                            binding.imageAttachment.setImageResource(R.drawable.ic_music_box_preview_24dp)
                        }
                        else -> {
                            binding.imageAttachment.visibility = View.VISIBLE
                            binding.textAttachment.visibility = View.GONE

                            binding.imageAttachment.setChecked(!media.description.isNullOrEmpty())
                            binding.imageAttachment.setProgress(media.uploadPercent)

                            Glide.with(binding.imageAttachment.context)
                                .load(media.uri)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .dontAnimate()
                                .into(binding.imageAttachment)
                        }
                    }

                    binding.attachmentLayout.visibility = View.VISIBLE
                } else {
                    binding.attachmentLayout.visibility = View.GONE
                }
            }
            viewModel.uploadError.observe {
                displayTransientError(R.string.error_media_upload_sending)
            }
        }
    }

    private fun setEmojiList(emojiList: List<Emoji>?) {
        if (emojiList != null) {
            binding.emojiView.adapter = EmojiAdapter(
                emojiList,
                this@ChatActivity,
                preferences.getBoolean(PrefKeys.ANIMATE_CUSTOM_EMOJIS, false)
            )
            enableButton(binding.emojiButton, true, emojiList.isNotEmpty())
        }
    }

    private fun replaceTextAtCaret(text: CharSequence) {
        // If you select "backward" in an editable, you get SelectionStart > SelectionEnd
        val start = binding.editText.selectionStart.coerceAtMost(binding.editText.selectionEnd)
        val end = binding.editText.selectionStart.coerceAtLeast(binding.editText.selectionEnd)
        val textToInsert = if (start > 0 && !binding.editText.text[start - 1].isWhitespace()) {
            " $text"
        } else {
            text
        }
        binding.editText.text.replace(start, end, textToInsert)

        // Set the cursor after the inserted text
        binding.editText.setSelection(start + text.length)
    }

    override fun onEmojiSelected(shortcode: String) {
        replaceTextAtCaret(":$shortcode: ")
    }

    override fun onEmojiSelected(id: String, shortcode: String) {
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

    private fun setupButtons() {
        addMediaBehavior = BottomSheetBehavior.from(binding.addMediaBottomSheet)
        emojiBehavior = BottomSheetBehavior.from(binding.emojiView)
        stickerBehavior = BottomSheetBehavior.from(binding.stickerKeyboard)

        binding.sendButton.setOnClickListener { onSendClicked() }

        binding.attachmentButton.setOnClickListener { openPickDialog() }
        binding.emojiButton.setOnClickListener { showEmojis() }
        if (viewModel.tryFetchStickers) {
            binding.stickerButton.setOnClickListener { showStickers() }
            binding.stickerButton.visibility = View.VISIBLE
            enableButton(binding.stickerButton, false, false)
        } else {
            binding.stickerButton.visibility = View.GONE
        }

        binding.emojiView.layoutManager =
            GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false)

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

        binding.actionPhotoTake.setOnClickListener { initiateCameraApp() }
        binding.actionPhotoPick.setOnClickListener { onMediaPick() }
    }

    private fun onSendClicked() {
        val media = viewModel.getSingleMedia()

        serviceClient.sendChatMessage(
            MessageToSend(
                binding.editText.text.toString(),
                media?.id,
                media?.uri?.toString(),
                accountManager.value.activeAccount!!.id,
                this.chatId,
                0
            )
        )

        sending = true
        binding.editText.text.clear()
        viewModel.media.value = listOf()
        enableButton(binding.sendButton, false, false)
        enableButton(binding.attachmentButton, false, false)
        enableButton(binding.stickerButton, false, false)
    }

    private fun openPickDialog() {
        if (addMediaBehavior.state == BottomSheetBehavior.STATE_HIDDEN || addMediaBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            addMediaBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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
                    addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                } else {
                    emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }
    }

    private fun showStickers() {
        if (stickerBehavior.state == BottomSheetBehavior.STATE_HIDDEN || stickerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            stickerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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

    private fun onMediaPick() {
        addMediaBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Wait until bottom sheet is not collapsed and show next screen after
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    addMediaBehavior.removeBottomSheetCallback(this)
                    if (ContextCompat.checkSelfPermission(
                            this@ChatActivity,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@ChatActivity,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateMediaPicking()
            } else {
                val bar = Snackbar.make(
                    binding.activityChat,
                    R.string.error_media_upload_permission,
                    Snackbar.LENGTH_SHORT
                ).apply {
                }
                bar.setAction(R.string.action_retry) { onMediaPick() }
                // necessary so snackbar is shown over everything
                bar.view.elevation =
                    resources.getDimension(R.dimen.compose_activity_snackbar_elevation)
                bar.show()
            }
        }
    }

    private fun initiateMediaPicking() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        intent.type = "*/*" // Pleroma allows anything
        startActivityForResult(intent, MEDIA_PICK_RESULT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK && requestCode == MEDIA_PICK_RESULT && intent != null) {
            pickMedia(intent.data!!)
        } else if (resultCode == Activity.RESULT_OK && requestCode == MEDIA_TAKE_PHOTO_RESULT) {
            pickMedia(photoUploadUri!!)
        }
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

    private fun pickMedia(
        uri: Uri,
        contentInfoCompat: InputContentInfoCompat? = null,
        filename: String? = null
    ) {
        withLifecycleContext {
            viewModel.pickMedia(uri, filename ?: uri.toFileName(contentResolver))
                .observe { exceptionOrItem ->

                    contentInfoCompat?.releasePermission()

                    if (exceptionOrItem.isLeft()) {
                        val errorId = when (val exception = exceptionOrItem.asLeft()) {
                            is VideoSizeException -> {
                                R.string.error_video_upload_size
                            }
                            is MediaSizeException -> {
                                R.string.error_media_upload_size
                            }
                            is AudioSizeException -> {
                                R.string.error_audio_upload_size
                            }
                            is VideoOrImageException -> {
                                R.string.error_media_upload_image_or_video
                            }
                            else -> {
                                Timber.e("That file could not be opened", exception)
                                R.string.error_media_upload_opening
                            }
                        }
                        displayTransientError(errorId)
                    }
                }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(PHOTO_UPLOAD_URI_KEY, photoUploadUri)
        outState.putString(MESSAGE_KEY, binding.editText.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun displayTransientError(@StringRes stringId: Int) {
        val bar = Snackbar.make(binding.activityChat, stringId, Snackbar.LENGTH_LONG)
        // necessary so snackbar is shown over everything
        bar.view.elevation = resources.getDimension(R.dimen.compose_activity_snackbar_elevation)
        bar.show()
    }

    private fun clearPlaceholdersForResponse(msgs: MutableList<ChatMesssageOrPlaceholder>) {
        msgs.removeAll { it.isLeft() }
    }

    private fun tryCache() {
        // Request timeline from disk to make it quick, then replace it with timeline from
        // the server to update it
        chatsRepo.getChatMessages(chatId, null, null, null, LOAD_AT_ONCE, TimelineRequestMode.DISK)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe { msgs ->
                if (msgs.size > 1) {
                    val mutableMsgs = msgs.toMutableList()
                    clearPlaceholdersForResponse(mutableMsgs)
                    this.msgs.clear()
                    this.msgs.addAll(mutableMsgs)
                    updateAdapter()
                    binding.progressBar.visibility = View.GONE
                    // Request statuses including current top to refresh all of them
                }
                updateCurrent()
                loadAbove()
            }
    }

    private fun updateCurrent() {
        if (msgs.isEmpty()) {
            return
        }

        val topId = msgs.first { it.isRight() }.asRight().id
        chatsRepo.getChatMessages(
            chatId,
            topId,
            null,
            null,
            LOAD_AT_ONCE,
            TimelineRequestMode.NETWORK
        )
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe({ messages ->
                initialUpdateFailed = false
                // When cached timeline is too old, we would replace it with nothing
                if (messages.isNotEmpty()) {
                    // clear old cached statuses
                    if (this.msgs.isNotEmpty()) {
                        this.msgs.removeAll {
                            if (it.isRight()) {
                                val chat = it.asRight()
                                chat.id.length < topId.length || chat.id < topId
                            } else {
                                val placeholder = it.asLeft()
                                placeholder.id.length < topId.length || placeholder.id < topId
                            }
                        }
                    }
                    this.msgs.addAll(messages)
                    updateAdapter()
                }
                bottomLoading = false
            }, {
                initialUpdateFailed = true
                // Indicate that we are not loading anymore
                binding.progressBar.visibility = View.GONE
            })
    }

    private fun showNothing() {
        binding.messageView.visibility = View.VISIBLE
        binding.messageView.setup(R.drawable.elephant_friend_empty, R.string.message_empty, null)
    }

    private fun loadAbove() {
        var firstOrNull: String? = null
        var secondOrNull: String? = null
        for (i in msgs.indices) {
            val msg = msgs[i]
            if (msg.isRight()) {
                firstOrNull = msg.asRight().id
                if (i + 1 < msgs.size && msgs[i + 1].isRight()) {
                    secondOrNull = msgs[i + 1].asRight().id
                }
                break
            }
        }
        if (firstOrNull != null) {
            sendFetchMessagesRequest(null, firstOrNull, secondOrNull, FetchEnd.TOP, -1)
        } else {
            sendFetchMessagesRequest(null, null, null, FetchEnd.BOTTOM, -1)
        }
    }

    private fun sendFetchMessagesRequest(
        maxId: String?,
        sinceId: String?,
        sinceIdMinusOne: String?,
        fetchEnd: FetchEnd,
        pos: Int
    ) {
        // allow getting old statuses/fallbacks for network only for for bottom loading
        val mode = if (fetchEnd == FetchEnd.BOTTOM) {
            TimelineRequestMode.ANY
        } else {
            TimelineRequestMode.NETWORK
        }
        chatsRepo.getChatMessages(chatId, maxId, sinceId, sinceIdMinusOne, LOAD_AT_ONCE, mode)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe(
                { result -> onFetchTimelineSuccess(result.toMutableList(), fetchEnd, pos) },
                { onFetchTimelineFailure(Exception(it), fetchEnd, pos) }
            )
    }

    private fun updateAdapter() {
        Timber.d("updateAdapter")
        differ.submitList(msgs.pairedCopy)
    }

    private fun updateMessages(
        newMsgs: MutableList<ChatMesssageOrPlaceholder>,
        fullFetch: Boolean
    ) {
        if (newMsgs.isEmpty()) {
            updateAdapter()
            return
        }
        if (msgs.isEmpty()) {
            msgs.addAll(newMsgs)
        } else {
            val lastOfNew = newMsgs[newMsgs.size - 1]
            val index = msgs.indexOf(lastOfNew)
            if (index >= 0) {
                msgs.subList(0, index).clear()
            }
            val newIndex = newMsgs.indexOf(msgs[0])
            if (newIndex == -1) {
                if (index == -1 && fullFetch) {
                    newMsgs.findLast { it.isRight() }?.let {
                        val placeholderId = it.asRight().id.inc()
                        newMsgs.add(Either.Left(Placeholder(placeholderId)))
                    }
                }
                msgs.addAll(0, newMsgs)
            } else {
                msgs.addAll(0, newMsgs.subList(0, newIndex))
            }
        }
        // Remove all consecutive placeholders
        removeConsecutivePlaceholders()
        updateAdapter()
    }

    private fun removeConsecutivePlaceholders() {
        for (i in 0 until msgs.size - 1) {
            if (msgs[i].isLeft() && msgs[i + 1].isLeft()) {
                msgs.removeAt(i)
            }
        }
    }

    private fun replacePlaceholderWithMessages(
        newMsgs: MutableList<ChatMesssageOrPlaceholder>,
        fullFetch: Boolean,
        pos: Int
    ) {
        val placeholder = msgs[pos]
        if (placeholder.isLeft()) {
            msgs.removeAt(pos)
        }
        if (newMsgs.isEmpty()) {
            updateAdapter()
            return
        }
        if (fullFetch) {
            newMsgs.add(placeholder)
        }
        msgs.addAll(pos, newMsgs)
        removeConsecutivePlaceholders()
        updateAdapter()
    }

    private fun addItems(newMsgs: List<ChatMesssageOrPlaceholder>) {
        if (newMsgs.isEmpty()) {
            return
        }
        val last = msgs.findLast { it.isRight() }

        // I was about to replace findStatus with indexOf but it is incorrect to compare value
        // types by ID anyway and we should change equals() for Status, I think, so this makes sense
        if (last != null && !newMsgs.contains(last)) {
            msgs.addAll(newMsgs)
            removeConsecutivePlaceholders()
            updateAdapter()
        }
    }

    private fun onFetchTimelineSuccess(
        msgs: MutableList<ChatMesssageOrPlaceholder>,
        fetchEnd: FetchEnd,
        pos: Int
    ) {
        // We filled the hole (or reached the end) if the server returned less statuses than we
        // we asked for.
        val fullFetch = msgs.size >= LOAD_AT_ONCE

        when (fetchEnd) {
            FetchEnd.TOP -> {
                updateMessages(msgs, fullFetch)

                val last = msgs.indexOfFirst { it.isRight() }

                mastodonApi.markChatAsRead(chatId, msgs[last].asRight().id)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(this, Lifecycle.Event.ON_DESTROY)
                    .subscribe({
                        Timber.d("Marked new messages as read up to ${msgs[last].asRight().id}")
                    }, {
                        Timber.e("Failed to mark messages as read", it)
                    })
            }
            FetchEnd.MIDDLE -> {
                replacePlaceholderWithMessages(msgs, fullFetch, pos)
            }
            FetchEnd.BOTTOM -> {
                if (this.msgs.isNotEmpty() && !this.msgs.last().isRight()) {
                    this.msgs.removeAt(this.msgs.size - 1)
                    updateAdapter()
                }

                if (msgs.isNotEmpty() && !msgs.last().isRight()) {
                    // Removing placeholder if it's the last one from the cache
                    msgs.removeAt(msgs.size - 1)
                }

                val oldSize = this.msgs.size
                if (this.msgs.size > 1) {
                    addItems(msgs)
                } else {
                    updateMessages(msgs, fullFetch)
                }

                if (this.msgs.size == oldSize) {
                    // This may be a brittle check but seems like it works
                    // Can we check it using headers somehow? Do all server support them?
                    didLoadEverythingBottom = true
                }
            }
        }
        updateBottomLoadingState(fetchEnd)
        binding.progressBar.visibility = View.GONE
        if (this.msgs.size == 0) {
            showNothing()
        } else {
            binding.messageView.visibility = View.GONE
        }
    }

    private fun onRefresh() {
        binding.messageView.visibility = View.GONE
        isNeedRefresh = false

        if (this.initialUpdateFailed) {
            updateCurrent()
        }
        loadAbove()
    }

    private fun onFetchTimelineFailure(exception: Exception, fetchEnd: FetchEnd, position: Int) {
        if (fetchEnd == FetchEnd.MIDDLE && !msgs[position].isRight()) {
            var placeholder = msgs[position].asLeftOrNull()
            val newViewData: ChatMessageViewData
            if (placeholder == null) {
                val msg = msgs[position - 1].asRight()
                val newId = msg.id.dec()
                placeholder = Placeholder(newId)
            }
            newViewData = ChatMessageViewData.Placeholder(placeholder.id, false)
            msgs.setPairedItem(position, newViewData)
            updateAdapter()
        } else if (msgs.isEmpty()) {
            binding.messageView.visibility = View.VISIBLE
            if (exception is IOException) {
                binding.messageView.setup(R.drawable.elephant_offline, R.string.error_network) {
                    binding.progressBar.visibility = View.VISIBLE
                    onRefresh()
                }
            } else {
                binding.messageView.setup(R.drawable.elephant_error, R.string.error_generic) {
                    binding.progressBar.visibility = View.VISIBLE
                    onRefresh()
                }
            }
        }
        Timber.e("Fetch Failure: " + exception.message)
        updateBottomLoadingState(fetchEnd)
        binding.progressBar.visibility = View.GONE
    }

    private fun updateBottomLoadingState(fetchEnd: FetchEnd) {
        if (fetchEnd == FetchEnd.BOTTOM) {
            bottomLoading = false
        }
    }

    override fun onLoadMore(position: Int) {
        // check bounds before accessing list,
        if (msgs.size >= position && position > 0) {
            val fromChat = msgs[position - 1].asRightOrNull()
            val toChat = msgs[position + 1].asRightOrNull()
            if (fromChat == null || toChat == null) {
                Timber.e("Failed to load more at $position, wrong placeholder position")
                return
            }

            val maxMinusOne =
                if (msgs.size > position + 1 && msgs[position + 2].isRight()) msgs[position + 1].asRight().id else null
            sendFetchMessagesRequest(
                fromChat.id,
                toChat.id,
                maxMinusOne,
                FetchEnd.MIDDLE,
                position
            )

            val (id) = msgs[position].asLeft()
            val newViewData = ChatMessageViewData.Placeholder(id, true)
            msgs.setPairedItem(position, newViewData)
            updateAdapter()
        } else {
            Timber.e("error loading more")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Timber.d(event.toString())
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.isCtrlPressed) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    // send message by pressing CTRL + ENTER
                    onSendClicked()
                    return true
                }
            }

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                onBackPressed()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        // Acting like a teen: deliberately ignoring parent.
        if (addMediaBehavior.state != BottomSheetBehavior.STATE_HIDDEN ||
            emojiBehavior.state != BottomSheetBehavior.STATE_HIDDEN ||
            stickerBehavior.state != BottomSheetBehavior.STATE_HIDDEN
        ) {
            addMediaBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            emojiBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            stickerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            return
        }

        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        startUpdateTimestamp()
    }

    /**
     * Start to update adapter every minute to refresh timestamp
     * If setting absoluteTimeView is false
     * Auto dispose observable on pause
     */
    private fun startUpdateTimestamp() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val useAbsoluteTime = preferences.getBoolean("absoluteTimeView", false)
        if (!useAbsoluteTime) {
            Observable.interval(1, TimeUnit.MINUTES)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(this, Lifecycle.Event.ON_PAUSE)
                .subscribe { updateAdapter() }
        }
    }

    override fun onViewAccount(id: String) {
        viewAccount(id)
    }

    override fun onViewUrl(url: String) {
        viewUrl(url)
    }

    override fun onViewTag(tag: String) {
        val intent = Intent(this, ViewTagActivity::class.java)
        intent.putExtra("hashtag", tag)
        startActivity(intent)
    }

    override fun onViewMedia(position: Int, view: View?) {
        val attachment = msgs[position].asRight().attachment!!

        when (attachment.type) {
            Attachment.Type.GIFV, Attachment.Type.VIDEO, Attachment.Type.AUDIO, Attachment.Type.IMAGE -> {
                val intent = ViewMediaActivity.newIntent(this, attachment)
                if (view != null) {
                    val url = attachment.url
                    ViewCompat.setTransitionName(view, url)
                    val options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, url)

                    startActivity(intent, options.toBundle())
                } else {
                    startActivity(intent)
                }
            }
            Attachment.Type.UNKNOWN -> {
                viewUrl(attachment.url)
            }
        }
    }

    companion object {
        private const val MEDIA_PICK_RESULT = 1
        private const val MEDIA_TAKE_PHOTO_RESULT = 2
        private const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1
        private const val PHOTO_UPLOAD_URI_KEY = "PHOTO_UPLOAD_URI"
        private const val MESSAGE_KEY = "MESSAGE"

        fun getIntent(context: Context, chat: Chat): Intent {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra(ID, chat.id)
            intent.putExtra(AVATAR_URL, chat.account.avatar)
            intent.putExtra(DISPLAY_NAME, chat.account.displayName ?: chat.account.localUsername)
            intent.putParcelableArrayListExtra(
                EMOJIS,
                ArrayList(chat.account.emojis ?: emptyList<Emoji>())
            )
            intent.putExtra(USERNAME, chat.account.username)
            return intent
        }

        const val ID = "id"
        const val AVATAR_URL = "avatar_url"
        const val DISPLAY_NAME = "display_name"
        const val USERNAME = "username"
        const val EMOJIS = "emojis"
    }
}
