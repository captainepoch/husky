package com.keylesspalace.tusky.components.lists.account.model

sealed class ListForAccountError {

    data class UnknownError(
        val listId: String = "",
        val action: ListsForAccountErrorAction
    ) : ListForAccountError()

    data class NetworkError(
        val listId: String = "",
        val action: ListsForAccountErrorAction
    ) : ListForAccountError()
}
