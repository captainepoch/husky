/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2023  The Husky Developers
 * Copyright (C) 2019  kyori19
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

package com.keylesspalace.tusky.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.databinding.ItemAutocompleteHashtagBinding
import com.keylesspalace.tusky.entity.MastoList

class ListSelectionAdapter(context: Context) : ArrayAdapter<MastoList>(
    context,
    R.layout.item_autocomplete_hashtag
) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView != null) {
            ItemAutocompleteHashtagBinding.bind(convertView)
        } else {
            ItemAutocompleteHashtagBinding.inflate(LayoutInflater.from(context), parent, false)
        }

        getItem(position)?.let { list ->
            binding.root.text = list.title
        }

        return binding.root
    }
}
