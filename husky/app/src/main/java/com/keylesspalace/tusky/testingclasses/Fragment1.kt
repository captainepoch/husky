package com.keylesspalace.tusky.testingclasses

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

class Fragment1(
    private val emojis: List<Emoji>
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
            layoutManager =
                GridLayoutManager(context, 3, GridLayoutManager.HORIZONTAL, false)
            adapter = EmojiAdapter(
                emojis,
                object : OnEmojiSelectedListener {
                    override fun onEmojiSelected(shortcode: String) {

                    }
                },
                false // TODO
            )
        }
    }
}
