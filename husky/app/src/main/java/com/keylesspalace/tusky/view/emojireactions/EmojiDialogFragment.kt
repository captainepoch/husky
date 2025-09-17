package com.keylesspalace.tusky.view.emojireactions

import android.app.Dialog
import android.os.Bundle
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

class EmojiDialogFragment(
    private val emojis: List<Emoji>?,
    private val onEmojiClick: (isCustomEmoji: Boolean, shortcode: String) -> Unit
) : DialogFragment() {

    companion object {
        private const val EMOJI_DIALOG_ITEM_COUNT = 2
        const val DIALOG_TAG = "CUSTOM_EMOJI_DIALOG"
    }

    private lateinit var binding: LayoutEmojiBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = LayoutEmojiBinding.inflate(layoutInflater)

        val viewPager = binding.dialogViewPager.apply {
            adapter = DialogViewPagerAdapter(requireActivity(), emojis) { isCustomEmoji, shortcode ->
                dismissAllowingStateLoss()
                onEmojiClick(isCustomEmoji, shortcode)
            }
            isUserInputEnabled = false

            val heightInPixels = (300 * resources.displayMetrics.density).toInt()
            layoutParams = layoutParams.apply {
                height = heightInPixels
            }
        }

        TabLayoutMediator(
            binding.dialogTabLayout, viewPager
        ) { tab, position ->
            tab.setIcon(
                when (position) {
                    0 -> {
                        R.drawable.neocat_aww
                    }
                    1 -> {
                        R.drawable.neocat_confused
                    }
                    else -> {
                        throw Exception("It shouldn't be more than two items")
                    }
                }
            )
        }.attach()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        return dialog
    }

    override fun onResume() {
        super.onResume()

        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }

    class DialogViewPagerAdapter(
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
                0 -> {
                    CustomEmojiPickerPage(emojis ?: emptyList()) { shortcode ->
                        onEmojiClick(true, shortcode)
                    }
                }
                1 -> {
                    UnicodeEmojiPickerPage { emoji ->
                        onEmojiClick(false, emoji)
                    }
                }
                else -> {
                    throw Exception("It shouldn't be more than two items")
                }
            }
        }
    }
}
