/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2017  Andrew Dawson
 *
 * This file contains parts of the ViewExtensions class from Tusky.
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

package com.keylesspalace.tusky.core.extensions

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

open class DefaultTextWatcher : TextWatcher {
    override fun afterTextChanged(s: Editable) {
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }
}

inline fun EditText.onTextChanged(
    crossinline callback: (s: CharSequence, start: Int, before: Int, count: Int) -> Unit
) {
    addTextChangedListener(object : DefaultTextWatcher() {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            callback(s, start, before, count)
        }
    })
}

inline fun EditText.afterTextChanged(
    crossinline callback: (s: Editable) -> Unit
) {
    addTextChangedListener(object : DefaultTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            callback(s)
        }
    })
}
