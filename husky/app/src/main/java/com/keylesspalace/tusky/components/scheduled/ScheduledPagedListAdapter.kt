/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2023  The Husky Developers
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

package com.keylesspalace.tusky.components.scheduled

import androidx.recyclerview.widget.DiffUtil
import com.keylesspalace.tusky.entity.ScheduledStatus

class ScheduledPagedListAdapter : DiffUtil.ItemCallback<ScheduledStatus>() {
    override fun areItemsTheSame(oldItem: ScheduledStatus, newItem: ScheduledStatus): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ScheduledStatus,
        newItem: ScheduledStatus
    ): Boolean {
        return oldItem == newItem
    }
}
