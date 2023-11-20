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

import java.util.regex.Pattern

/**
 * Returns an empty String.
 *
 * @return An empty string.
 */
val String.Companion.Empty
    inline get() = ""

/**
 * Add https protocol to the URL.
 *
 * @return The url with https:// in front, null otherwise.
 */
fun String?.addHttpsProtocolUrl(): String {
    if (this != null) {
        if (!this.contains("https", true)) {
            return ("https://$this")
        }
    }

    return String.Empty
}

/**
 * Returns the text with emojis and zero-width space characters at the start and end positions.
 *
 * @return String with zero-width space characters at start and end positions for emojis.
 */
fun String.composeWithZwsp(): String {
    val zwspChar = '\u200b'
    val pattern = Pattern.compile("(:)([a-zA-Z0-9_]*)(:( )?(\\R)?)")
    val matcher = pattern.matcher(this)

    var end: Int
    val originalString = StringBuilder(this)
    while (matcher.find()) {
        end = matcher.end()

        if (end < originalString.length) {
            val endChar = originalString[end - 1]

            if (endChar.isWhitespace()) {
                if (!originalString[end].isLetterOrDigit() && !endChar.isBreakline()) {
                    originalString.setCharAt(end - 1, zwspChar)
                }
            }
        }
    }

    return originalString.toString().trim()
}
