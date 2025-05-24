/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
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

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior

fun View.isVisible(): Boolean {
    return (this.visibility == View.VISIBLE)
}

fun View.isInvisible(): Boolean {
    return (this.visibility == View.INVISIBLE)
}

fun View.isGone(): Boolean {
    return (this.visibility == View.GONE)
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

// Add insets to the BottomSheetBehavior views
fun View.bottomSheetBehaviorWithIntents(): BottomSheetBehavior<View> {
    val bottomSheetBehavior = BottomSheetBehavior.from(this)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        bottomSheetBehavior.peekHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom + view.height
        insets
    }
    return bottomSheetBehavior
}
