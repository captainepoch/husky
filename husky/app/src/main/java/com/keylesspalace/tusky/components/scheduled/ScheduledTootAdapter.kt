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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.core.extensions.formatDate
import com.keylesspalace.tusky.databinding.ItemScheduledTootBinding
import com.keylesspalace.tusky.entity.ScheduledStatus

class ScheduledTootAdapter(
    val listener: ScheduledTootActionListener
) : PagedListAdapter<ScheduledStatus, ScheduledTootAdapter.TootViewHolder>(
    ScheduledPagedListAdapter()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TootViewHolder {
        return TootViewHolder(
            ItemScheduledTootBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: TootViewHolder, position: Int) {
        getItem(position)?.let {
            viewHolder.bind(it)
        }
    }

    inner class TootViewHolder(private val binding: ItemScheduledTootBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(item: ScheduledStatus) {
            binding.edit.isEnabled = true
            binding.delete.isEnabled = true
            binding.text.text = item.params.text
            binding.date.text = item.scheduledAt.formatDate()

            binding.edit.setOnClickListener { v: View ->
                v.isEnabled = false
                listener.edit(item)
            }

            binding.delete.setOnClickListener { v: View ->
                v.isEnabled = false
                listener.delete(item)
            }
        }
    }
}
