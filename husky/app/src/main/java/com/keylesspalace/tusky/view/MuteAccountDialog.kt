/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2020  Tusky Contributors
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

@file:JvmName("MuteAccountDialog")

package com.keylesspalace.tusky.view

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.databinding.DialogMuteAccountBinding

fun showMuteAccountDialog(
    activity: Activity,
    accountUsername: String,
    onOk: (notifications: Boolean, duration: Int) -> Unit
) {
    val binding = DialogMuteAccountBinding.inflate(activity.layoutInflater)
    binding.warning.text = activity.getString(R.string.dialog_mute_warning, accountUsername)
    binding.checkbox.isChecked = false

    AlertDialog.Builder(activity)
        .setView(binding.root)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            val durationValues = activity.resources.getIntArray(R.array.mute_duration_values)
            onOk(binding.checkbox.isChecked, durationValues[binding.duration.selectedItemPosition])
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}
