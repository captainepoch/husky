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

package com.keylesspalace.tusky.adapter

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView.BufferType.SPANNABLE
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.keylesspalace.tusky.databinding.ItemEmojiReactionBinding
import com.keylesspalace.tusky.entity.EmojiReaction
import com.keylesspalace.tusky.interfaces.StatusActionListener
import com.keylesspalace.tusky.util.createEmojiSpan

class EmojiReactionsAdapter internal constructor(
    private val reactions: List<EmojiReaction>,
    private val listener: StatusActionListener,
    private val statusId: String
) : Adapter<SingleViewHolder>() {

    private lateinit var binding: ItemEmojiReactionBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleViewHolder {
        binding = ItemEmojiReactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SingleViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: SingleViewHolder, position: Int) {
        val reaction = reactions[position]
        val builder = SpannableStringBuilder("${reaction.name} ${reaction.count}")

        reaction.url?.let { url ->
            builder.setSpan(
                createEmojiSpan(url, binding.emojiReactionButton, true),
                0,
                reaction.name.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.emojiReactionButton.apply {
            setText(builder, SPANNABLE)
            isActivated = reaction.me
            setOnClickListener { v: View? ->
                v?.let {
                    listener.onEmojiReactMenu(it, reaction, statusId)
                }
            }
        }
    }

    // Number of rows
    override fun getItemCount(): Int {
        return reactions.size
    }
}
