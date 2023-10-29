package com.keylesspalace.tusky.components.lists.account.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keylesspalace.tusky.components.lists.account.model.ListForAccount
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountState
import com.keylesspalace.tusky.components.lists.domain.ListsRepository
import com.keylesspalace.tusky.core.extensions.cancelIfActive
import com.keylesspalace.tusky.core.functional.Either.Left
import com.keylesspalace.tusky.core.functional.Either.Right
import com.keylesspalace.tusky.entity.MastoList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

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
                }.catch {
                }.collect { result ->
                    when (result) {
                        is Right -> {
                            loadListsForAccount(result.value)
                        }

                        is Left -> {

                        }
                    }
                }
        }
    }

    private suspend fun loadListsForAccount(lists: List<MastoList>) {
        repository.getListsIncludesAccount(userAccountId)
            .onStart {
            }.catch {
            }.collect { result ->
                when (result) {
                    is Right -> {
                        _state.emit(
                            ListsForAccountState(
                                listsForAccount = lists.map { list ->
                                    ListForAccount(
                                        list = list,
                                        accountIsIncluded = result.value.any { it.id == list.id }
                                    )
                                }
                            )
                        )
                    }

                    is Left -> {

                    }
                }
            }
    }

    fun addAccountToList(listId: String) {
        job.cancelIfActive()
        job = viewModelScope.launch(Dispatchers.IO) {
            repository.addAccountToList(listId, listOf(userAccountId))
                .onStart {
                }.catch {
                    Timber.d("Error: ${it.cause?.message}")
                }.collect { result ->
                    when (result) {
                        is Right -> {
                            _state.value = ListsForAccountState(
                                listsForAccount = _state.value.listsForAccount.map { listItem ->
                                    if (listId == listItem.list.id) {
                                        listItem.copy(accountIsIncluded = true)
                                    } else {
                                        listItem
                                    }
                                }
                            )
                        }

                        is Left -> {
                            Timber.e("Result is error [${result.value}]")
                        }
                    }
                }
        }
    }
}
