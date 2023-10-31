package com.keylesspalace.tusky.components.lists.account.model

data class ListsForAccountState(
    val isLoading: Boolean = false,
    val error: ListForAccountError? = null,
    val listsForAccount: List<ListForAccount> = emptyList()
)
