/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2023  The Husky Developers
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

import java.text.SimpleDateFormat
import java.util.Locale

fun String.formatDate(
    regex: String = "yyyy-MM-dd'T'HH:mm:ss.sss'Z'",
    newFormat: String = "yyyy-MM-dd HH:mm"
): String {
    return runCatching {
        SimpleDateFormat(regex, Locale.getDefault()).parse(this)
    }.map { date ->
        if (date != null) {
            SimpleDateFormat(newFormat, Locale.getDefault()).format(date)
        } else {
            this
        }
    }.getOrDefault(this)
}
