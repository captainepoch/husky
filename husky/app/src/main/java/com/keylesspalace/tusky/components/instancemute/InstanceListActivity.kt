package com.keylesspalace.tusky.components.instancemute

import android.os.Bundle
import android.view.MenuItem
import com.keylesspalace.tusky.BaseActivity
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.components.instancemute.fragment.InstanceListFragment
import kotlinx.android.synthetic.main.toolbar_basic.toolbar

class InstanceListActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_list)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.title_domain_mutes)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, InstanceListFragment())
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
}
