package com.keylesspalace.tusky.view.emojireactions

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.adapter.OnEmojiSelectedListener
import com.keylesspalace.tusky.core.extensions.afterTextChanged
import com.keylesspalace.tusky.core.extensions.gone
import com.keylesspalace.tusky.databinding.LayoutEmojiCustomBinding
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.settings.PrefKeys
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomEmojiPickerPage(
    private val emojiList: List<Emoji>,
    private val onEmojiClick: (String) -> Unit
) : Fragment() {

    private lateinit var binding: LayoutEmojiCustomBinding
    private val preferences: SharedPreferences by inject()
    private val customEmojiViewModel by viewModel<CustomEmojiPickerViewModel>()
    private val adapter by lazy {
        ListEmojiAdapter(
            object : OnEmojiSelectedListener {
                override fun onEmojiSelected(shortcode: String) {
                    onEmojiClick(shortcode)
                }
            },
            animateEmojis = preferences.getBoolean(PrefKeys.ANIMATE_CUSTOM_EMOJIS, false)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutEmojiCustomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = GridLayoutManager(context, 1)
        binding.emojiGrid.layoutManager = layoutManager

        binding.emojiGrid.viewTreeObserver.addOnGlobalLayoutListener {
            val totalWidth = binding.emojiGrid.width
            val itemWidthPx = (48) * resources.displayMetrics.density
            val spanCount = maxOf(1, (totalWidth / itemWidthPx).toInt())
            (binding.emojiGrid.layoutManager as GridLayoutManager).spanCount = spanCount
        }

        binding.searchBox.setOnClickListener {
            binding.searchBox.requestFocus()
        }

        binding.searchBox.afterTextChanged { text ->
            customEmojiViewModel.updateQuery(text.toString())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                customEmojiViewModel.emojis.collect { emojis ->
                    binding.emojiGrid.adapter = adapter
                    adapter.submitList(emojis)

                    binding.loadingOverlay.gone()
                }
            }
        }

        customEmojiViewModel.setEmojis(emojiList)
    }
}
