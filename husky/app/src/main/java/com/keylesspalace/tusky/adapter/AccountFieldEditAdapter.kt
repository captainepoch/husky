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
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.databinding.ItemEditFieldBinding
import com.keylesspalace.tusky.entity.StringField

class AccountFieldEditAdapter : RecyclerView.Adapter<AccountFieldEditAdapter.ViewHolder>() {

    private val fieldData = mutableListOf<MutableStringPair>()

    fun setFields(fields: List<StringField>) {
        fieldData.clear()

        fields.forEach { field ->
            fieldData.add(MutableStringPair(field.name, field.value))
        }

        if (fieldData.isEmpty()) {
            fieldData.add(MutableStringPair("", ""))
        }

        notifyDataSetChanged()
    }

    fun getFieldData(): List<StringField> {
        return fieldData.map {
            StringField(it.first, it.second)
        }
    }

    fun addField() {
        fieldData.add(MutableStringPair("", ""))
        notifyItemInserted(fieldData.size - 1)
    }

    override fun getItemCount(): Int = fieldData.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemEditFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.nameTextView.setText(fieldData[position].first)
        viewHolder.valueTextView.setText(fieldData[position].second)

        viewHolder.nameTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(newText: Editable) {
                fieldData[viewHolder.absoluteAdapterPosition].first = newText.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        viewHolder.valueTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(newText: Editable) {
                fieldData[viewHolder.absoluteAdapterPosition].second = newText.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    class ViewHolder(view: ItemEditFieldBinding) : RecyclerView.ViewHolder(view.root) {
        val nameTextView: EditText = view.accountFieldName
        val valueTextView: EditText = view.accountFieldValue
    }

    class MutableStringPair(var first: String, var second: String)
}
