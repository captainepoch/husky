/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2017  Andrew Dawson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.keylesspalace.tusky

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityAccountListBinding
import com.keylesspalace.tusky.fragment.AccountListFragment
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class AccountListActivity : BaseActivity(), HasAndroidInjector {

    private val binding by viewBinding(ActivityAccountListBinding::inflate)

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    enum class Type {
        FOLLOWS,
        FOLLOWERS,
        BLOCKS,
        MUTES,
        FOLLOW_REQUESTS,
        REBLOGGED,
        FAVOURITED,
        REACTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val type = intent.getSerializableExtra(EXTRA_TYPE) as Type
        val id: String? = intent.getStringExtra(EXTRA_ID)
        val emoji: String? = intent.getStringExtra(EXTRA_EMOJI)

        setSupportActionBar(binding.includedToolbar.toolbar)
        supportActionBar?.apply {
            when (type) {
                Type.BLOCKS -> setTitle(R.string.title_blocks)
                Type.MUTES -> setTitle(R.string.title_mutes)
                Type.FOLLOW_REQUESTS -> setTitle(R.string.title_follow_requests)
                Type.FOLLOWERS -> setTitle(R.string.title_followers)
                Type.FOLLOWS -> setTitle(R.string.title_follows)
                Type.REBLOGGED -> setTitle(R.string.title_reblogged_by)
                Type.FAVOURITED -> setTitle(R.string.title_favourited_by)
                Type.REACTED -> setTitle(
                    String.format(
                        getString(R.string.title_emoji_reacted_by),
                        emoji
                    )
                )
            }
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, AccountListFragment.newInstance(type, id, emoji))
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun androidInjector() = dispatchingAndroidInjector

    companion object {
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_ID = "id"
        private const val EXTRA_EMOJI = "emoji"

        @JvmStatic
        fun newIntent(context: Context, type: Type, id: String?, emoji: String?): Intent {
            return Intent(context, AccountListActivity::class.java).apply {
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_EMOJI, emoji)
            }
        }

        @JvmStatic
        fun newIntent(context: Context, type: Type, id: String? = null): Intent {
            return newIntent(context, type, id, null)
        }
    }
}
