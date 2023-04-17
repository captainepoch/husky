/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2018  Conny Duck
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

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.databinding.ItemEditFieldBinding
import com.keylesspalace.tusky.entity.StringField

class AccountFieldEditAdapter : ListAdapter<MutableStringPair, AccountFieldViewHolder>(
    AccountFieldsDiff
) {

    internal var clearFieldListener: (Int) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountFieldViewHolder {
        val view = ItemEditFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountFieldViewHolder(view)
    }

    override fun getItemCount(): Int = currentList.size

    override fun onBindViewHolder(viewHolder: AccountFieldViewHolder, position: Int) {
        viewHolder.nameTextView.setText(currentList[position].first)
        viewHolder.valueTextView.setText(currentList[position].second)
        viewHolder.deleteField.setOnClickListener {
            clearFieldListener(position)
        }

        viewHolder.nameTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(newText: Editable) {
                currentList[viewHolder.absoluteAdapterPosition].first = newText.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        viewHolder.valueTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(newText: Editable) {
                currentList[viewHolder.absoluteAdapterPosition].second = newText.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    fun getFieldData(): List<StringField> {
        return currentList.map {
            StringField(it.first, it.second)
        }
    }

    private object AccountFieldsDiff : DiffUtil.ItemCallback<MutableStringPair>() {

        override fun areItemsTheSame(
            oldItem: MutableStringPair,
            newItem: MutableStringPair
        ): Boolean {
            return (oldItem == newItem)
        }

        override fun areContentsTheSame(
            oldItem: MutableStringPair,
            newItem: MutableStringPair
        ): Boolean {
            return (oldItem.first == newItem.first &&
                    oldItem.second == newItem.second)
        }
    }
}

class AccountFieldViewHolder(view: ItemEditFieldBinding) : RecyclerView.ViewHolder(view.root) {

    val nameTextView: EditText = view.accountFieldName
    val valueTextView: EditText = view.accountFieldValue
    val deleteField: ImageButton = view.deleteField
}

class MutableStringPair(var first: String, var second: String)
