package com.keylesspalace.tusky.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.util.AttributeSet
import android.util.Log
import androidx.emoji.widget.EmojiAppCompatTextView
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.util.EmojiSpan
import com.keylesspalace.tusky.util.LinkHelper
import com.keylesspalace.tusky.util.emojify

/*
 * This is a TextView that changes break strategy to simple
 * if there is too much custom emojis
 *
 * Fixes Android performance bug
 */

class CustomEmojiTextView
@JvmOverloads constructor(context:Context,
                          attrs: AttributeSet? = null,
                          defStyleAttr: Int = 0
): EmojiAppCompatTextView(context, attrs, defStyleAttr) {
    private var oldBreakStrategy = 1 // Layout.BREAK_STRATEGY_HIGH_QUALITY

    @SuppressLint("WrongConstant")
    override fun setText(text: CharSequence?, type: BufferType?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var overridden = false

            // don't change if break strategy already simple
            if(text is Spannable && breakStrategy != Layout.BREAK_STRATEGY_SIMPLE) {
                val spans = text.getSpans(0, text.length, EmojiSpan::class.java)

                if (spans.size >= SPAN_LIMIT) {
                    oldBreakStrategy = breakStrategy
                    breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
                    overridden = true

                    Log.d("CustomEmojiTextView", "break strategy overriden!");
                }
            }

            if(!overridden)
                breakStrategy = oldBreakStrategy
        }

        super.setText(text, type)
    }

    companion object {
        const val SPAN_LIMIT = 100 // heuristics
    }
}

