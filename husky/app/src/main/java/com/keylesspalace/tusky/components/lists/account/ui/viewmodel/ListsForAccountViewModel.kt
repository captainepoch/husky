package com.keylesspalace.tusky.components.lists.account.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keylesspalace.tusky.components.lists.account.model.ListForAccount
import com.keylesspalace.tusky.components.lists.account.model.ListForAccountError
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountErrorAction
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountErrorAction.ADD
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountErrorAction.DEL
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountErrorAction.LOAD
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountState
import com.keylesspalace.tusky.components.lists.domain.ListsRepository
import com.keylesspalace.tusky.core.extensions.cancelIfActive
import com.keylesspalace.tusky.core.functional.Either.Left
import com.keylesspalace.tusky.core.functional.Either.Right
import com.keylesspalace.tusky.entity.MastoList
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class ListsForAccountViewModel(
    private val repository: ListsRepository
) : ViewModel() {

    private var job: Job? = null

    private val _state = MutableStateFlow(ListsForAccountState())
    val state = _state.asStateFlow()

    var userAccountId = ""

    fun loadLists() {
        job.cancelIfActive()
        job = viewModelScope.launch(Dispatchers.IO) {
            repository.getLists()
                .onStart {
                    resetState(true)
                }.catch { failure ->
                    checkIfNetworkError(failure, "", LOAD)
                }.collect { result ->
                    when (result) {
                        is Right -> {
                            loadListsForAccount(result.value)
                        }

                        is Left -> {
                            resetState(false, result.value)
                        }
                    }
                }
        }
    }

    private suspend fun loadListsForAccount(lists: List<MastoList>) {
        repository.getListsIncludesAccount(userAccountId)
            .onStart {
                resetState(true)
            }.catch { failure ->
                checkIfNetworkError(failure, "", LOAD)
            }.collect { result ->
                when (result) {
                    is Right -> {
                        _state.value = ListsForAccountState(
                            listsForAccount = lists.map { list ->
                                ListForAccount(
                                    list = list,
                                    accountIsIncluded = result.value.any { it.id == list.id }
                                )
                            }
                        )
                    }

                    is Left -> {
                        resetState(false, result.value)
                    }
                }
            }
    }

    fun addAccountToList(listId: String) {
        job.cancelIfActive()
        job = viewModelScope.launch(Dispatchers.IO) {
            repository.addAccountToList(listId, listOf(userAccountId))
                .onStart {
                    resetState(true)
                }.catch { failure ->
                    checkIfNetworkError(failure, listId, ADD)
                }.collect { result ->
                    when (result) {
                        is Right -> {
                            updateAccountList(listId, true)
                        }

                        is Left -> {
                            resetState(false, result.value)
                        }
                    }
                }
        }
    }

    fun removeAccountFromList(listId: String) {
        job.cancelIfActive()
        job = viewModelScope.launch(Dispatchers.IO) {
            repository.removeAccountFromList(listId, listOf(userAccountId))
                .onStart {
                    resetState(true)
                }.catch { failure ->
                    checkIfNetworkError(failure, listId, DEL)
                }.collect { result ->
                    when (result) {
                        is Right -> {
                            updateAccountList(listId, false)
                        }

                        is Left -> {
                            resetState(false, result.value)
                        }
                    }
                }
        }
    }

    private fun resetState(isLoading: Boolean, error: ListForAccountError? = null) {
        _state.value = _state.value.copy(
            isLoading = isLoading,
            error = error
        )
    }

    private fun checkIfNetworkError(
        throwable: Throwable?,
        listId: String = "",
        action: ListsForAccountErrorAction
    ) {
        if (throwable is IOException) {
            resetState(false, ListForAccountError.NetworkError(listId, action))
        } else {
            resetState(false, ListForAccountError.UnknownError(listId, action))
        }
    }

    private fun updateAccountList(listId: String, accountIsIncluded: Boolean) {
        _state.value = ListsForAccountState(
            listsForAccount = _state.value.listsForAccount.map { listItem ->
                if (listId == listItem.list.id) {
                    listItem.copy(accountIsIncluded = accountIsIncluded)
                } else {
                    listItem
                }
            }
        )
    }
}
