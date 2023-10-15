package com.keylesspalace.tusky.components.lists.account.model

import com.keylesspalace.tusky.core.functional.CustomError

data class ListsForAccountState(
    val isLoading: Boolean = false,
    val error: CustomError? = null,
    val listsForAccount: List<ListForAccount> = emptyList()
)
