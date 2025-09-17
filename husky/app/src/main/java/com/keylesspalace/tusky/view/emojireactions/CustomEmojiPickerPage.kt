package com.keylesspalace.tusky.view.emojireactions

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.keylesspalace.tusky.adapter.OnEmojiSelectedListener
import com.keylesspalace.tusky.core.extensions.afterTextChanged
import com.keylesspalace.tusky.core.extensions.gone
import com.keylesspalace.tusky.databinding.LayoutEmojiCustomBinding
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.settings.PrefKeys
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomEmojiPickerPage : Fragment() {

    private lateinit var binding: LayoutEmojiCustomBinding
    private val preferences: SharedPreferences by inject()
    private val customEmojiViewModel by viewModel<CustomEmojiPickerViewModel>()
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var initialEmojis: List<Emoji>? = null
    private lateinit var onEmojiClick: (String) -> Unit

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

    companion object {
        fun newInstance(
            emojiList: List<Emoji>,
            onEmojiClick: (String) -> Unit
        ): CustomEmojiPickerPage {
            return CustomEmojiPickerPage().apply {
                this.initialEmojis = emojiList
                this.onEmojiClick = onEmojiClick
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialEmojis?.let { list ->
            customEmojiViewModel.setEmojis(list)
            initialEmojis = null
        }
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

        setupGridLayoutListener()

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
    }

    private fun setupGridLayoutListener() {
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (!isAdded || context == null) {
                return@OnGlobalLayoutListener
            }

            val totalWidth = binding.emojiGrid.width
            if (totalWidth > 0) {
                val itemWidthPx = (48 * resources.displayMetrics.density).toInt()
                val spanCount = maxOf(1, totalWidth / itemWidthPx)
                (binding.emojiGrid.layoutManager as? GridLayoutManager)?.spanCount = spanCount
            }
        }

        binding.emojiGrid.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    override fun onDestroyView() {
        globalLayoutListener?.let { listener ->
            binding.emojiGrid.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
        globalLayoutListener = null

        super.onDestroyView()
    }
}
