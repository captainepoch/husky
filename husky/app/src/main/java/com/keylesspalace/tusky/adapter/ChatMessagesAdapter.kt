package com.keylesspalace.tusky.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.interfaces.ChatActionListener
import com.keylesspalace.tusky.util.StatusDisplayOptions
import com.keylesspalace.tusky.util.ThemeUtils
import com.keylesspalace.tusky.util.TimestampUtils
import com.keylesspalace.tusky.util.emojify
import com.keylesspalace.tusky.util.LinkHelper
import com.keylesspalace.tusky.view.MediaPreviewImageView
import com.keylesspalace.tusky.viewdata.ChatMessageViewData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ChatMessagesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    object Key {
        const val KEY_CREATED = "created"
    }

    private val content: TextView = view.findViewById(R.id.content)
    private val timestamp: TextView = view.findViewById(R.id.datetime)
    private val attachmentView: MediaPreviewImageView = view.findViewById(R.id.attachment)
    private val mediaOverlay: ImageView = view.findViewById(R.id.mediaOverlay)
    private val attachmentLayout: FrameLayout = view.findViewById(R.id.attachmentLayout)

    private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val mediaPreviewUnloaded = ColorDrawable(ThemeUtils.getColor(itemView.context, R.attr.colorBackgroundAccent))

    fun setupWithChatMessage(msg: ChatMessageViewData.Concrete, chatActionListener: ChatActionListener, payload: Any?) {
        if(payload == null) {
            if(msg.content != null) {
                val text = msg.content.emojify(msg.emojis, content)
                LinkHelper.setClickableText(content, text, null, chatActionListener)
            }

            setAttachment(msg.attachment, chatActionListener)
            setCreatedAt(msg.createdAt)
        } else {
            if(payload is List<*>) {
                for (item in payload) {
                    if (ChatsViewHolder.Key.KEY_CREATED == item) {
                        setCreatedAt(msg.createdAt)
                    }
                }
            }
        }
    }

    private fun loadImage(imageView: MediaPreviewImageView,
                          previewUrl: String?,
                          meta: Attachment.MetaData?) {
        if (TextUtils.isEmpty(previewUrl)) {
            imageView.removeFocalPoint()
            Glide.with(imageView)
                    .load(mediaPreviewUnloaded)
                    .centerInside()
                    .into(imageView)
        } else {
            val focus = meta?.focus
            if (focus != null) { // If there is a focal point for this attachment:
                imageView.setFocalPoint(focus)
                Glide.with(imageView)
                        .load(previewUrl)
                        .placeholder(mediaPreviewUnloaded)
                        .centerInside()
                        .addListener(imageView)
                        .into(imageView)
            } else {
                imageView.removeFocalPoint()
                Glide.with(imageView)
                        .load(previewUrl)
                        .placeholder(mediaPreviewUnloaded)
                        .centerInside()
                        .into(imageView)
            }
        }
    }

    private fun formatDuration(durationInSeconds: Double): String? {
        val seconds = durationInSeconds.roundToInt().toInt() % 60
        val minutes = durationInSeconds.toInt() % 3600 / 60
        val hours = durationInSeconds.toInt() / 3600
        return String.format("%d:%02d:%02d", hours, minutes, seconds)
    }

    private fun getAttachmentDescription(context: Context, attachment: Attachment): CharSequence {
        var duration = ""
        if (attachment.meta?.duration != null && attachment.meta.duration > 0) {
            duration = formatDuration(attachment.meta.duration.toDouble()) + " "
        }
        return if (TextUtils.isEmpty(attachment.description)) {
            duration + context.getString(R.string.description_status_media_no_description_placeholder)
        } else {
            duration + attachment.description
        }
    }


    private fun setAttachmentClickListener(view: View, listener: ChatActionListener, attachment: Attachment, animateTransition: Boolean) {
        view.setOnClickListener { v: View ->
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onViewMedia(position, if (animateTransition) v else null)
            }
        }
        view.setOnLongClickListener { v: View ->
            val description = getAttachmentDescription(v.context, attachment)
            Toast.makeText(v.context, description, Toast.LENGTH_LONG).show()
            true
        }
    }


    private fun setAttachment(attachment: Attachment?, listener: ChatActionListener) {
        if(attachment == null) {
            attachmentLayout.visibility = View.GONE
        } else {
            attachmentLayout.visibility = View.VISIBLE

            val previewUrl = attachment.previewUrl
            val description = attachment.description

            if(TextUtils.isEmpty(description)) {
                attachmentView.contentDescription = description
            } else {
                attachmentView.contentDescription = attachmentView.context
                        .getString(R.string.action_view_media)
            }

            loadImage(attachmentView, previewUrl, attachment.meta)

            when(attachment.type) {
                Attachment.Type.VIDEO, Attachment.Type.GIFV -> {
                    mediaOverlay.visibility = View.VISIBLE
                }
                else -> {
                    mediaOverlay.visibility = View.GONE
                }
            }

            setAttachmentClickListener(attachmentView, listener, attachment, true)
        }
    }

    private fun setCreatedAt(createdAt: Date) {
        timestamp.text = sdf.format(createdAt)
    }
}

class ChatMessagesAdapter(private val dataSource : TimelineAdapter.AdapterDataSource<ChatMessageViewData>,
                          private val chatActionListener: ChatActionListener,
                          private val localUserId: String)
: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_OUR_MESSAGE = 0
    private val VIEW_TYPE_THEIR_MESSAGE = 1
    private val VIEW_TYPE_PLACEHOLDER = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(viewType) {
            VIEW_TYPE_OUR_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_our_message, parent, false)
                return ChatMessagesViewHolder(view)
            }
            VIEW_TYPE_THEIR_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_their_message, parent, false)
                return ChatMessagesViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_status_placeholder, parent, false)
                return PlaceholderViewHolder(view)
            }
        }
    }

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
        val chat: ChatMessageViewData = dataSource.getItemAt(position)
        if(holder is PlaceholderViewHolder) {
            holder.setup(chatActionListener, (chat as ChatMessageViewData.Placeholder).isLoading)
        } else if(holder is ChatMessagesViewHolder) {
            holder.setupWithChatMessage(chat as ChatMessageViewData.Concrete, chatActionListener,
                    if (payloads != null && payloads.isNotEmpty()) payloads[0] else null)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(dataSource.getItemAt(position) is ChatMessageViewData.Concrete) {
            val msg = dataSource.getItemAt(position) as ChatMessageViewData.Concrete

            if(msg.accountId == localUserId) {
                return VIEW_TYPE_OUR_MESSAGE
            }
            return VIEW_TYPE_THEIR_MESSAGE
        }
        return VIEW_TYPE_PLACEHOLDER
    }

    override fun getItemId(position: Int): Long {
        return dataSource.getItemAt(position).getViewDataId().toLong()
    }
}
