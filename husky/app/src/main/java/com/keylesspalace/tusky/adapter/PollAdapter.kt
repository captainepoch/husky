/* Copyright 2019 Conny Duck
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
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.emoji2.text.EmojiCompat
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.util.emojify
import com.keylesspalace.tusky.util.visible
import com.keylesspalace.tusky.viewdata.PollOptionViewData
import com.keylesspalace.tusky.viewdata.buildDescription
import com.keylesspalace.tusky.viewdata.calculatePercent

class PollAdapter : RecyclerView.Adapter<PollViewHolder>() {

    private var pollOptions: List<PollOptionViewData> = emptyList()
    private var voteCount: Int = 0
    private var votersCount: Int? = null
    private var mode = RESULT
    private var emojis: List<Emoji> = emptyList()
    private var resultClickListener: View.OnClickListener? = null

    fun setup(
        options: List<PollOptionViewData>,
        voteCount: Int,
        votersCount: Int?,
        emojis: List<Emoji>,
        mode: Int,
        resultClickListener: View.OnClickListener?
    ) {
        this.pollOptions = options
        this.voteCount = voteCount
        this.votersCount = votersCount
        this.emojis = emojis
        this.mode = mode
        this.resultClickListener = resultClickListener
        notifyDataSetChanged()
    }

    fun getSelected(): List<Int> {
        return pollOptions.filter { it.selected }
            .map { pollOptions.indexOf(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollViewHolder {
        return PollViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_poll,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return pollOptions.size
    }

    override fun onBindViewHolder(holder: PollViewHolder, position: Int) {
        val option = pollOptions[position]

        holder.resultTextView.visible(mode == RESULT)
        holder.radioButton.visible(mode == SINGLE)
        holder.checkBox.visible(mode == MULTIPLE)

        when (mode) {
            RESULT -> {
                val percent = calculatePercent(option.votesCount, voteCount)
                val emojifiedPollOptionText =
                    buildDescription(option.title, percent, holder.resultTextView.context)
                        .emojify(emojis, holder.resultTextView)
                holder.resultTextView.text = EmojiCompat.get().process(emojifiedPollOptionText)

                val level = percent * 100

                holder.resultTextView.background.level = level
                holder.resultTextView.setOnClickListener(resultClickListener)
            }
            SINGLE -> {
                val emojifiedPollOptionText = option.title.emojify(emojis, holder.radioButton)
                holder.radioButton.text = EmojiCompat.get().process(emojifiedPollOptionText)
                holder.radioButton.isChecked = option.selected
                holder.radioButton.setOnClickListener {
                    pollOptions.forEachIndexed { index, pollOption ->
                        pollOption.selected = index == holder.adapterPosition
                        notifyItemChanged(index)
                    }
                }
            }
            MULTIPLE -> {
                val emojifiedPollOptionText = option.title.emojify(emojis, holder.checkBox)
                holder.checkBox.text = EmojiCompat.get().process(emojifiedPollOptionText)
                holder.checkBox.isChecked = option.selected
                holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                    pollOptions[holder.adapterPosition].selected = isChecked
                }
            }
        }
    }

    companion object {
        const val RESULT = 0
        const val SINGLE = 1
        const val MULTIPLE = 2
    }
}

class PollViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val resultTextView: TextView = view.findViewById(R.id.status_poll_option_result)
    val radioButton: RadioButton = view.findViewById(R.id.status_poll_radio_button)
    val checkBox: CheckBox = view.findViewById(R.id.status_poll_checkbox)
}
