/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2019  Tusky Contributors
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

package com.keylesspalace.tusky.service

import android.content.Intent
import android.os.Build.VERSION_CODES
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.keylesspalace.tusky.MainActivity

/**
 * This adds a tile in the QuickSettings menu.
 *
 * You can press it and it opens the Compose activity or shows an account selector when
 * multiple accounts are present.
 */
@RequiresApi(VERSION_CODES.N)
class HuskyTileService : TileService() {

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = Intent.ACTION_SEND
            type = "text/plain"
        }
        startActivityAndCollapse(intent)
    }
}
