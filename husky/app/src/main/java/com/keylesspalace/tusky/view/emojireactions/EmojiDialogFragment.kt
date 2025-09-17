package com.keylesspalace.tusky.view.emojireactions

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.databinding.LayoutEmojiBinding
import com.keylesspalace.tusky.entity.Emoji

class EmojiDialogFragment : DialogFragment() {

    companion object {
        private const val EMOJI_DIALOG_ITEM_COUNT = 2
        private const val DIALOG_SELECTED_TAB = "selected_tab"
        const val DIALOG_TAG = "CUSTOM_EMOJI_DIALOG"

        fun newInstance(
            emojis: List<Emoji>?,
            onEmojiClick: (isCustomEmoji: Boolean, shortcode: String) -> Unit
        ): EmojiDialogFragment {
            return EmojiDialogFragment().apply {
                this.emojis = emojis
                this.onEmojiClick = onEmojiClick
            }
        }
    }

    private lateinit var binding: LayoutEmojiBinding
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var selectedTabPosition = 0
    private var emojis: List<Emoji>? = null
    private var onEmojiClick: ((Boolean, String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = LayoutEmojiBinding.inflate(layoutInflater)

        selectedTabPosition = savedInstanceState?.getInt(DIALOG_SELECTED_TAB) ?: 0

        setupViewPager()
        setupDialog()
        calculateHeight()

        return createDialog()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        selectedTabPosition = binding.dialogViewPager.currentItem

        binding.dialogViewPager.post {
            calculateHeight()

            binding.dialogViewPager.post {
                if (selectedTabPosition < EMOJI_DIALOG_ITEM_COUNT) {
                    binding.dialogViewPager.setCurrentItem(selectedTabPosition, false)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        selectedTabPosition = binding.dialogViewPager.currentItem
        outState.putInt(DIALOG_SELECTED_TAB, selectedTabPosition)
    }

    override fun onDestroyView() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null

        super.onDestroyView()
    }

    private fun setupViewPager() {
        binding.dialogViewPager.apply {
            adapter = DialogViewPagerAdapter(requireActivity(), emojis) { isCustomEmoji, shortcode ->
                dismissAllowingStateLoss()
                onEmojiClick?.invoke(isCustomEmoji, shortcode)
            }
            isUserInputEnabled = false
        }
    }

    private fun setupDialog() {
        tabLayoutMediator = TabLayoutMediator(binding.dialogTabLayout, binding.dialogViewPager) { tab, position ->
            tab.setIcon(
                when (position) {
                    0 -> R.drawable.neocat_aww
                    1 -> R.drawable.neocat_confused
                    else -> throw Exception("It shouldn't be more than two items")
                }
            )
        }
        tabLayoutMediator?.attach()

        if (selectedTabPosition < EMOJI_DIALOG_ITEM_COUNT) {
            binding.dialogViewPager.post {
                binding.dialogViewPager.setCurrentItem(selectedTabPosition, false)
            }
        }
    }

    private fun createDialog(): Dialog {
        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        return dialog
    }

    private fun calculateHeight() {
        val dialogHeight = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            (300 * resources.displayMetrics.density).toInt()
        }

        binding.dialogViewPager.layoutParams = binding.dialogViewPager.layoutParams.apply {
            height = dialogHeight
        }
    }

    inner class DialogViewPagerAdapter(
        activity: FragmentActivity,
        private val emojis: List<Emoji>?,
        private val onEmojiClick: (isCustomEmoji: Boolean, shortcode: String) -> Unit
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int {
            return EMOJI_DIALOG_ITEM_COUNT
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            return itemId in 0 until itemCount
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CustomEmojiPickerPage.newInstance(emojis ?: emptyList()) { shortcode ->
                    onEmojiClick(true, shortcode)
                }

                1 -> UnicodeEmojiPickerPage.newInstance { emoji ->
                    onEmojiClick(false, emoji)
                }

                else -> throw IllegalStateException("It shouldn't be more than two items")
            }
        }
    }
}
