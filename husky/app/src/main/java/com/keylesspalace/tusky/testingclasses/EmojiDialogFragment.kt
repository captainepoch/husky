package com.keylesspalace.tusky.testingclasses

import android.app.Dialog
import android.os.Bundle
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
    private val onReactionCallback: (String) -> Unit
) : DialogFragment() {

    companion object {
        const val DIALOG_TAG = "CUSTOM_EMOJI_DIALOG"
    }

    private lateinit var binding: LayoutEmojiBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = LayoutEmojiBinding.inflate(layoutInflater)

        val viewPager = binding.dialogViewPager.apply {
            adapter = DialogViewPagerAdapter(requireActivity(), emojis) { shortcode ->
                dismissAllowingStateLoss()
                onReactionCallback(shortcode)
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
                        R.drawable.arrows_clockwise
                    }
                    1 -> {
                        R.drawable.ic_alert_circle
                    }
                    else -> {
                        throw Exception("It shouldn't be more than two items")
                    }
                }
            )
        }.attach()

        return AlertDialog.Builder(requireContext()).setView(binding.root).create()
    }

    class DialogViewPagerAdapter(
        activity: FragmentActivity,
        private val emojis: List<Emoji>?,
        private val onReactionCallback: (String) -> Unit
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int {
            return 2
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
                        onReactionCallback(shortcode)
                    }
                }

                1 -> {
                    UnicodeEmojiPickerPage { emoji ->
                        onReactionCallback(emoji)
                    }
                }
                else -> {
                    throw Exception("It shouldn't be more than two items")
                }
            }
        }
    }
}
