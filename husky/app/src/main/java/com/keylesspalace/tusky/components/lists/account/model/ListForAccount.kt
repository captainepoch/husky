package com.keylesspalace.tusky.components.lists.account.model

import com.keylesspalace.tusky.entity.MastoList

data class ListForAccount(
    val list: MastoList,
    val accountIsIncluded: Boolean
)
