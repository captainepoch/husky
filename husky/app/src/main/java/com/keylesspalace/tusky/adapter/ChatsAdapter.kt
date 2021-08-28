package com.keylesspalace.tusky.adapter

import android.graphics.Typeface
import android.opengl.Visibility
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.toSpanned
import androidx.recyclerview.widget.RecyclerView
import at.connyduck.sparkbutton.helpers.Utils
import com.bumptech.glide.Glide
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.interfaces.AccountActionListener
import com.keylesspalace.tusky.interfaces.ChatActionListener
import com.keylesspalace.tusky.util.StatusDisplayOptions
import com.keylesspalace.tusky.util.TimestampUtils
import com.keylesspalace.tusky.util.emojify
import com.keylesspalace.tusky.util.loadAvatar
import com.keylesspalace.tusky.viewdata.ChatViewData
import java.text.SimpleDateFormat
import java.util.*

class ChatsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    object Key {
        const val KEY_CREATED = "created"
    }

    private val avatar: ImageView = view.findViewById(R.id.status_avatar)
    private val avatarInset: ImageView = view.findViewById(R.id.status_avatar_inset)
    private val displayName: TextView = view.findViewById(R.id.status_display_name)
    private val userName: TextView = view.findViewById(R.id.status_username)
    private val timestamp: TextView = view.findViewById(R.id.status_timestamp_info)
    private val content: TextView = view.findViewById(R.id.status_content)
    private val unread: TextView = view.findViewById(R.id.chat_unread)

    private val shortSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val longSdf = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())

    fun setupWithChat(chat: ChatViewData.Concrete,
                      listener: ChatActionListener,
                      statusDisplayOptions: StatusDisplayOptions,
                      localUserId: String,
                      payload: Any?) {
        if (payload == null) {
            displayName.text = chat.account.displayName?.emojify(chat.account.emojis, displayName, true)
                    ?: ""
            userName.text = userName.context.getString(R.string.status_username_format, chat.account.username)
            setUpdatedAt(chat.updatedAt, statusDisplayOptions)
            setAvatar(chat.account.avatar, chat.account.bot, statusDisplayOptions)
            if (chat.unread <= 0) {
                unread.visibility = View.GONE
            } else if (chat.unread > 99) {
                unread.text = ":)"
            } else {
                unread.text = chat.unread.toString()
            }
            avatar.setOnClickListener { listener.onViewAccount(chat.account.id) }
            val onLongClickListener = View.OnLongClickListener {
                listener.onMore(chat.id, it)
                true
            }
            val onClickListener = View.OnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION)
                    listener.openChat(pos)
            }

            content.setOnLongClickListener(onLongClickListener)
            itemView.setOnLongClickListener(onLongClickListener)
            content.setOnClickListener(onClickListener)
            itemView.setOnClickListener(onClickListener)

            if(chat.lastMessage != null) {
                var text = if (chat.lastMessage.content != null) {
                    content.setTypeface(null, Typeface.NORMAL)

                    chat.lastMessage.content.emojify(chat.lastMessage.emojis, content, true)
                } else if (chat.lastMessage.attachment != null) {
                    content.setTypeface(null, Typeface.ITALIC)

                    content.resources.getString(chat.lastMessage.attachment.describeAttachmentType())
                } else if (chat.lastMessage.card != null) {
                    content.setTypeface(null, Typeface.ITALIC)

                    content.resources.getString(R.string.link)
                } else ""

                content.text = if(chat.lastMessage.accountId == localUserId) {
                    SpannableStringBuilder.valueOf(content.resources.getText(R.string.chat_our_last_message))
                            .append(": ").append(text)
                } else text

            } else {
                content.text = ""
            }
        } else {
            if(payload is List<*>) {
                for (item in payload as List<*>) {
                    if (Key.KEY_CREATED == item) {
                        setUpdatedAt(chat.updatedAt, statusDisplayOptions)
                    }
                }
            }
        }
    }

    private fun setAvatar(url: String,
                          isBot: Boolean,
                          statusDisplayOptions: StatusDisplayOptions) {
        avatar.setPaddingRelative(0, 0, 0, 0)
        if (statusDisplayOptions.showBotOverlay && isBot) {
            avatarInset.visibility = View.VISIBLE
            avatarInset.setBackgroundColor(0x50ffffff)
            Glide.with(avatarInset)
                .load(R.drawable.ic_bot_24dp)
                .into(avatarInset)
        } else {
            avatarInset.visibility = View.GONE
        }
        val avatarRadius = itemView.context.resources.getDimensionPixelSize(R.dimen.avatar_radius_48dp);
        loadAvatar(url, avatar, avatarRadius,
                statusDisplayOptions.animateAvatars)
    }


    private fun getAbsoluteTime(createdAt: Date?): String? {
        if (createdAt == null) {
            return "??:??:??"
        }
        return if (DateUtils.isToday(createdAt.time)) {
            shortSdf.format(createdAt)
        } else {
            longSdf.format(createdAt)
        }
    }

    private fun setUpdatedAt(updatedAt: Date, statusDisplayOptions: StatusDisplayOptions) {
        if (statusDisplayOptions.useAbsoluteTime) {
            timestamp.text = getAbsoluteTime(updatedAt)
        } else {
            val then = updatedAt.time
            val now = System.currentTimeMillis()
            val readout = TimestampUtils.getRelativeTimeSpanString(timestamp.context, then, now)
            timestamp.text = readout
        }
    }

}

class ChatsAdapter(private val dataSource: TimelineAdapter.AdapterDataSource<ChatViewData>,
                   val statusDisplayOptions: StatusDisplayOptions,
                   private val chatActionListener: ChatActionListener,
                   val localUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_CHAT = 0
    private val VIEW_TYPE_PLACEHOLDER = 1

    override fun getItemCount(): Int {
        return dataSource.itemCount
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        bindViewHolder(holder, position, null)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payload: MutableList<Any>) {
        bindViewHolder(holder, position, payload)
    }

    private fun bindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>?) {
        val chat: ChatViewData = dataSource.getItemAt(position)
        if(holder is PlaceholderViewHolder) {
            holder.setup(chatActionListener, (chat as ChatViewData.Placeholder).isLoading)
        } else if(holder is ChatsViewHolder) {
            holder.setupWithChat(chat as ChatViewData.Concrete, chatActionListener,
                    statusDisplayOptions, localUserId,
                    if (payloads != null && payloads.isNotEmpty()) payloads[0] else null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if(viewType == VIEW_TYPE_CHAT ) {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat, parent, false)
            return ChatsViewHolder(view)
        }
        // else VIEW_TYPE_PLACEHOLDER

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_status_placeholder, parent, false)
        return PlaceholderViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        if(dataSource.getItemAt(position) is ChatViewData.Concrete)
            return VIEW_TYPE_CHAT

        return VIEW_TYPE_PLACEHOLDER
    }

    override fun getItemId(position: Int): Long {
        return dataSource.getItemAt(position).getViewDataId().toLong()
    }
}