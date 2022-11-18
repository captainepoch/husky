/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2018  Conny Duck
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

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.annotation.RawRes
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityLicenseBinding
import com.keylesspalace.tusky.util.IOUtils
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LicenseActivity : BaseActivity() {

    private val binding by viewBinding(ActivityLicenseBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.includedToolbar.toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        setTitle(R.string.title_licenses)

        loadFileIntoTextView(R.raw.apache, binding.licenseApacheTextView)
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

    private fun loadFileIntoTextView(@RawRes fileId: Int, textView: TextView) {
        val sb = StringBuilder()
        val br = BufferedReader(InputStreamReader(resources.openRawResource(fileId)))

        try {
            var line: String? = br.readLine()
            while (line != null) {
                sb.append(line)
                sb.append('\n')
                line = br.readLine()
            }
        } catch (e: IOException) {
            Timber.w(e)
        }

        IOUtils.closeQuietly(br)
        textView.text = sb.toString()
    }
}
