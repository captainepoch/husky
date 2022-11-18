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

package com.keylesspalace.tusky.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.Layout
import android.text.Spannable
import android.util.AttributeSet
import androidx.emoji.widget.EmojiAppCompatTextView
import com.keylesspalace.tusky.util.EmojiSpan
import timber.log.Timber

/**
 * This is a TextView that changes the break strategy to simple if there are too much custom emojis
 * present.
 *
 * It fixes an Android performance bug.
 */

class CustomEmojiTextView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : EmojiAppCompatTextView(context, attrs, defStyleAttr) {

    private var oldBreakStrategy = Layout.BREAK_STRATEGY_SIMPLE

    @SuppressLint("WrongConstant")
    override fun setText(text: CharSequence?, type: BufferType?) {
        var overridden = false

        // Do not change if break strategy is already Layout.BREAK_STRATEGY_HIGH_QUALITY
        if (text is Spannable && breakStrategy != Layout.BREAK_STRATEGY_SIMPLE) {
            val spans = text.getSpans(0, text.length, EmojiSpan::class.java)

            if (spans.size >= SPAN_LIMIT) {
                oldBreakStrategy = breakStrategy
                breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
                overridden = true

                Timber.d("Break strategy overridden!")
            }
        }

        if (!overridden) {
            breakStrategy = oldBreakStrategy

            Timber.d("Setting old break strategy")
        }

        super.setText(text, type)
    }

    private companion object {
        // Heuristics
        const val SPAN_LIMIT = 100
    }
}
