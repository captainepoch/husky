package com.keylesspalace.tusky.testingclasses

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.keylesspalace.tusky.databinding.LayoutEmojiBinding
import com.keylesspalace.tusky.entity.Emoji

class EmojiDialogFragment(
    private val emojis: List<Emoji>?
) : DialogFragment() {

    private lateinit var binding: LayoutEmojiBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = LayoutEmojiBinding.inflate(layoutInflater)

        binding.dialogViewPager.adapter = DialogViewPagerAdapter(requireActivity(), emojis)

        TabLayoutMediator(
            binding.dialogTabLayout,
            binding.dialogViewPager
        ) { tab, position ->
            tab.text = "$position"
        }.attach()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    class DialogViewPagerAdapter(
        activity: FragmentActivity,
        private val emojis: List<Emoji>?
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> Fragment1(emojis ?: emptyList())
                1 -> Fragment2()
                else -> throw Exception("Nope")
            }
        }
    }
}
