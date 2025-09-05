package com.keylesspalace.tusky.view.emojireactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.penfeizhou.animation.glide.AnimationDecoderOption
import com.keylesspalace.tusky.adapter.OnEmojiSelectedListener
import com.keylesspalace.tusky.databinding.ItemEmojiButtonBinding
import com.keylesspalace.tusky.entity.Emoji

class ListEmojiAdapter(
    private val onEmojiSelectedListener: OnEmojiSelectedListener,
    private var animateEmojis: Boolean
) : ListAdapter<Emoji, ListEmojiAdapter.ListEmojiHolder>(EmojiDiffer) {

    private object EmojiDiffer : DiffUtil.ItemCallback<Emoji>() {
        override fun areItemsTheSame(oldItem: Emoji, newItem: Emoji): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Emoji, newItem: Emoji): Boolean {
            return ((oldItem.category == newItem.category) &&
                (oldItem.shortcode == newItem.shortcode) &&
                (oldItem.url == newItem.url) &&
                (oldItem.staticUrl == newItem.staticUrl) &&
                (oldItem.visibleInPicker == newItem.visibleInPicker))
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListEmojiHolder {
        return ListEmojiHolder(
            ItemEmojiButtonBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ListEmojiHolder,
        position: Int
    ) {
        val emoji = getItem(position)

        Glide.with(holder.layout.composeEmojiButton)
            .load(emoji.url)
            .set(AnimationDecoderOption.DISABLE_ANIMATION_GIF_DECODER, !animateEmojis)
            .set(AnimationDecoderOption.DISABLE_ANIMATION_WEBP_DECODER, !animateEmojis)
            .set(AnimationDecoderOption.DISABLE_ANIMATION_APNG_DECODER, !animateEmojis)
            .into(holder.layout.composeEmojiButton)

        holder.layout.composeEmojiButton.setOnClickListener {
            onEmojiSelectedListener.onEmojiSelected(emoji.shortcode)
        }

        holder.layout.composeEmojiButton.contentDescription = emoji.shortcode
    }

    inner class ListEmojiHolder(val layout: ItemEmojiButtonBinding) : RecyclerView.ViewHolder(layout.composeEmojiButton)
}
