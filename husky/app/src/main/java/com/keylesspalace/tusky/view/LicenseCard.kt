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

package com.keylesspalace.tusky.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.core.extensions.gone
import com.keylesspalace.tusky.databinding.CardLicenseBinding
import com.keylesspalace.tusky.util.LinkHelper
import com.keylesspalace.tusky.util.ThemeUtils

class LicenseCard
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val binding = CardLicenseBinding.inflate(LayoutInflater.from(context), this)

    init {
        setCardBackgroundColor(ThemeUtils.getColor(context, R.attr.colorSurface))

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.LicenseCard, 0, 0)

        val name: String? = a.getString(R.styleable.LicenseCard_name)
        val license: String? = a.getString(R.styleable.LicenseCard_license)
        val link: String? = a.getString(R.styleable.LicenseCard_link)
        a.recycle()

        binding.licenseCardName.text = name
        binding.licenseCardLicense.text = license
        if(link.isNullOrBlank()) {
            binding.licenseCardLink.gone()
        } else {
            binding.licenseCardLink.text = link
            setOnClickListener { LinkHelper.openLink(link, context) }
        }
    }
}
