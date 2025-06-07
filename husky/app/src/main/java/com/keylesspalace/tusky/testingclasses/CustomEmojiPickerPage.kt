package com.keylesspalace.tusky.testingclasses

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.keylesspalace.tusky.adapter.EmojiAdapter
import com.keylesspalace.tusky.adapter.OnEmojiSelectedListener
import com.keylesspalace.tusky.databinding.LayoutEmojiDialog1Binding
import com.keylesspalace.tusky.entity.Emoji

class CustomEmojiPickerPage(
    private val emojis: List<Emoji>,
    private val onReactionCallback: (String) -> Unit
) : Fragment() {

    private lateinit var binding: LayoutEmojiDialog1Binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutEmojiDialog1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.emojiGrid.apply {
            layoutManager = GridLayoutManager(context, calculateSpanCount(context), GridLayoutManager.VERTICAL, false)
            adapter = EmojiAdapter(
                emojis,
                object : OnEmojiSelectedListener {
                    override fun onEmojiSelected(shortcode: String) {
                        onReactionCallback(shortcode)
                    }
                },
                false // TODO
            )
        }
    }

    override fun onResume() {
        super.onResume()
        view?.post {
            binding.emojiGrid.requestLayout()
            binding.emojiGrid.invalidate()
        }
    }

    private fun calculateSpanCount(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val itemWidthPx = (40 * displayMetrics.density).toInt()
        return maxOf(1, ((screenWidthPx / itemWidthPx) - 2))
    }
}
