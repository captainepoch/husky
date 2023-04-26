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

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.databinding.ItemAccountFieldBinding
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.entity.Field
import com.keylesspalace.tusky.entity.IdentityProof
import com.keylesspalace.tusky.interfaces.LinkListener
import com.keylesspalace.tusky.util.Either
import com.keylesspalace.tusky.util.LinkHelper
import com.keylesspalace.tusky.util.emojify

class AccountFieldAdapter(private val linkListener: LinkListener) :
    RecyclerView.Adapter<AccountFieldAdapter.ViewHolder>() {

    var emojis: List<Emoji> = emptyList()
    var fields: List<Either<IdentityProof, Field>> = emptyList()

    override fun getItemCount() = fields.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemAccountFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val proofOrField = fields[position]

        if (proofOrField.isLeft()) {
            val identityProof = proofOrField.asLeft()

            viewHolder.nameTextView.text = identityProof.provider
            viewHolder.valueTextView.text =
                LinkHelper.createClickableText(identityProof.username, identityProof.profileUrl)

            viewHolder.valueTextView.movementMethod = LinkMovementMethod.getInstance()

            viewHolder.valueTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_check_circle,
                0
            )
        } else {
            val field = proofOrField.asRight()
            val emojifiedName = field.name.emojify(emojis, viewHolder.nameTextView)
            viewHolder.nameTextView.text = emojifiedName

            val emojifiedValue = field.value.emojify(emojis, viewHolder.valueTextView)
            LinkHelper.setClickableText(
                viewHolder.valueTextView,
                emojifiedValue,
                null,
                linkListener
            )

            if (field.verifiedAt != null) {
                viewHolder.valueTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_check_circle,
                    0
                )
            } else {
                viewHolder.valueTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
    }

    class ViewHolder(view: ItemAccountFieldBinding) : RecyclerView.ViewHolder(view.root) {
        val nameTextView: TextView = view.accountFieldName
        val valueTextView: TextView = view.accountFieldValue
    }
}
