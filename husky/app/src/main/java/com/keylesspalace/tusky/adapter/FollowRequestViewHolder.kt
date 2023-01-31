package com.keylesspalace.tusky.adapter

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.emoji.widget.EmojiTextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.databinding.ItemFollowRequestBinding
import com.keylesspalace.tusky.databinding.ItemFollowRequestNotificationBinding
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.interfaces.AccountActionListener
import com.keylesspalace.tusky.util.BindingViewHolder
import com.keylesspalace.tusky.util.emojify
import com.keylesspalace.tusky.util.loadAvatar
import com.keylesspalace.tusky.util.unicodeWrap
import com.keylesspalace.tusky.util.visible

class FollowRequestViewHolder(
    binding: ViewBinding,
    private val showHeader: Boolean
) : BindingViewHolder<ViewBinding>(binding) {

    private lateinit var displayNameTextView: EmojiTextView
    private var notificationTextView: EmojiTextView? = null
    private lateinit var usernameTextView: TextView
    private lateinit var avatar: ImageView
    private lateinit var acceptButton: ImageButton
    private lateinit var rejectButton: ImageButton

    private lateinit var id: String
    private val animateAvatar: Boolean =
        PreferenceManager.getDefaultSharedPreferences(binding.root.context)
            .getBoolean("animateGifAvatars", false)

    init {
        when (binding) {
            is ItemFollowRequestBinding -> {
                displayNameTextView = binding.displayNameTextView
                usernameTextView = binding.usernameTextView
                avatar = binding.avatar
                acceptButton = binding.acceptButton
                rejectButton = binding.rejectButton
            }
            is ItemFollowRequestNotificationBinding -> {
                notificationTextView = binding.notificationTextView
                displayNameTextView = binding.displayNameTextView
                usernameTextView = binding.usernameTextView
                avatar = binding.avatar
                acceptButton = binding.acceptButton
                rejectButton = binding.rejectButton
            }
        }
    }

    fun setupWithAccount(account: Account) {
        id = account.id
        val wrappedName = account.name.unicodeWrap()
        val emojifiedName: CharSequence = wrappedName.emojify(account.emojis, itemView, true)
        displayNameTextView.text = emojifiedName

        if (showHeader) {
            val wholeMessage: String =
                itemView.context.getString(R.string.notification_follow_request_format, wrappedName)
            notificationTextView?.text = SpannableStringBuilder(wholeMessage).apply {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    wrappedName.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }.emojify(account.emojis, itemView)
        }

        notificationTextView?.visible(showHeader)

        val format = itemView.context.getString(R.string.status_username_format)
        val formattedUsername = String.format(format, account.username)
        usernameTextView.text = formattedUsername

        val avatarRadius =
            avatar.context.resources.getDimensionPixelSize(R.dimen.avatar_radius_48dp)
        loadAvatar(account.avatar, avatar, avatarRadius, animateAvatar)
    }

    fun setupActionListener(listener: AccountActionListener) {
        acceptButton.setOnClickListener {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onRespondToFollowRequest(true, id, position)
            }
        }

        rejectButton.setOnClickListener {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onRespondToFollowRequest(false, id, position)
            }
        }

        itemView.setOnClickListener { listener.onViewAccount(id) }
    }
}
