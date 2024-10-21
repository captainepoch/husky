/* Copyright 2018 Conny Duck
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.penfeizhou.animation.glide.AnimationDecoderOption
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.entity.Emoji

class EmojiAdapter(
    emojiList: List<Emoji>,
    private val onEmojiSelectedListener: OnEmojiSelectedListener,
    private val animateEmojis: Boolean
) : RecyclerView.Adapter<EmojiAdapter.EmojiHolder>() {

    private val emojis: List<Emoji> = emojiList.filter { emoji -> emoji.visibleInPicker == null || emoji.visibleInPicker }
        .sortedBy { it.shortcode.lowercase() }

    override fun getItemCount(): Int {
        return emojis.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emoji_button, parent, false) as ImageView
        return EmojiHolder(view)
    }

    override fun onBindViewHolder(viewHolder: EmojiHolder, position: Int) {
        val emoji = emojis[position]

        Glide.with(viewHolder.emojiImageView)
            .load(emoji.url)
            .set(AnimationDecoderOption.DISABLE_ANIMATION_GIF_DECODER, !animateEmojis)
            .set(AnimationDecoderOption.DISABLE_ANIMATION_WEBP_DECODER, !animateEmojis)
            .set(AnimationDecoderOption.DISABLE_ANIMATION_APNG_DECODER, !animateEmojis)
            .into(viewHolder.emojiImageView)

        viewHolder.emojiImageView.setOnClickListener {
            onEmojiSelectedListener.onEmojiSelected(emoji.shortcode)
        }

        viewHolder.emojiImageView.contentDescription = emoji.shortcode
    }

    class EmojiHolder(val emojiImageView: ImageView) : RecyclerView.ViewHolder(emojiImageView)
}

interface OnEmojiSelectedListener {
    fun onEmojiSelected(shortcode: String)
}
