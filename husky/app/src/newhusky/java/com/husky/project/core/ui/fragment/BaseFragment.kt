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

package com.husky.project.core.ui.fragment

import androidx.annotation.LayoutRes
import com.zhuinden.simplestackextensions.fragments.KeyedFragment

open class BaseFragment(@LayoutRes layoutRes: Int) : KeyedFragment(layoutRes) {

    fun onHandleBack(): Boolean {
        return true
    }
}
