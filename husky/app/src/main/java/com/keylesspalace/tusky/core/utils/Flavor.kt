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

package com.keylesspalace.tusky.core.utils

enum class Flavor(private val flavor: String) {

    DEV("huskyDev"),
    BETA("huskyBeta"),
    STABLE("huskyStable"),
    NEW_HUSKY("huskyNewhusky");

    companion object {

        /**
         * Get the flavor enum (recommended use: using <code>Flavor.<FLAVOR></code>
         * (<FLAVOR>: BETA, STABLE).
         *
         * @param flavor The name of the Flavor.
         */
        fun getFlavor(flavor: String) =
            when (flavor) {
                DEV.flavor -> DEV
                BETA.flavor -> BETA
                NEW_HUSKY.flavor -> NEW_HUSKY
                else -> STABLE
            }
    }
}
