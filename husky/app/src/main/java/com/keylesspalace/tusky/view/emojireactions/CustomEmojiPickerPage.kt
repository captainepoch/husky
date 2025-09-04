package com.keylesspalace.tusky.view.emojireactions

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.keylesspalace.tusky.adapter.EmojiAdapter
import com.keylesspalace.tusky.adapter.OnEmojiSelectedListener
import com.keylesspalace.tusky.databinding.LayoutEmojiDialog1Binding
import com.keylesspalace.tusky.entity.Emoji
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomEmojiPickerPage(
    private val emojiList: List<Emoji>,
    private val onReactionCallback: (String) -> Unit
) : Fragment() {

    private lateinit var binding: LayoutEmojiDialog1Binding
    private val customEmojiViewModel by viewModel<CustomEmojiPickerViewModel>()

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

        binding.emojiGrid.layoutManager = GridLayoutManager(context, calculateSpanCount(requireContext()))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                customEmojiViewModel.emojis.collect { emojis ->
                    binding.emojiGrid.adapter = EmojiAdapter(
                        emojis,
                        object : OnEmojiSelectedListener {
                            override fun onEmojiSelected(shortcode: String) {
                                onReactionCallback(shortcode)
                            }
                        },
                        animateEmojis = false // TODO
                    )
                }
            }
        }

        customEmojiViewModel.setEmojis(emojiList)
    }

    private fun calculateSpanCount(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val itemWidthPx = (40 * displayMetrics.density).toInt()
        return maxOf(1, ((screenWidthPx / itemWidthPx) - 2))
    }
}
