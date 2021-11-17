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

import com.keylesspalace.tusky.BuildConfig

/**
 * Utils for getting application details.
 */
object ApplicationUtils {

    /**
     * Get if it's in DEBUG mode.
     *
     * @return True if DEBUG, false otherwise.
     */
    fun isDebug(): Boolean {
        return BuildConfig.DEBUG
    }

    /**
     * Get the flavor of the application.
     *
     * @return The flavor of the application.
     */
    fun getFlavor(): Flavor {
        return Flavor.getFlavor(BuildConfig.FLAVOR)
    }
}
