package com.keylesspalace.tusky.components.lists.account.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.components.lists.account.model.ListForAccountError
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountErrorAction
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountState
import com.keylesspalace.tusky.components.lists.account.ui.view.adapter.ListsForAccountAdapter
import com.keylesspalace.tusky.components.lists.account.ui.viewmodel.ListsForAccountViewModel
import com.keylesspalace.tusky.core.extensions.gone
import com.keylesspalace.tusky.core.extensions.visible
import com.keylesspalace.tusky.databinding.FragmentListsForAccountBinding
import com.keylesspalace.tusky.util.hide
import com.keylesspalace.tusky.util.show
import com.keylesspalace.tusky.util.visible
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ListsForAccountFragment : DialogFragment() {

    private lateinit var binding: FragmentListsForAccountBinding
    private val viewModel: ListsForAccountViewModel by viewModel()
    private val listsAccountAdapter by lazy {
        ListsForAccountAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.TuskyDialogFragmentStyle)

        requireArguments().getString(USER_ACCOUNT_ID)?.let { accountId ->
            viewModel.userAccountId = accountId
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListsForAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.accountLists.apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = listsAccountAdapter
        }

        setupListeners()
        setupObservers()

        viewModel.loadLists()
    }

    private fun setupListeners() {
        listsAccountAdapter.onListItemClick = { listId, accountIsIncluded ->
            if (accountIsIncluded) {
                viewModel.removeAccountFromList(listId)
            } else {
                viewModel.addAccountToList(listId)
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: ListsForAccountState) {
        binding.progressBar.visible(state.isLoading)

        if (state.isLoading) {
            return
        }

        if (state.error != null) {
            binding.accountLists.gone()
            handleError(state.error)

            return
        }

        listsAccountAdapter.submitList(state.listsForAccount)
        binding.accountLists.visible()
    }

    private fun handleError(error: ListForAccountError?) {
        when (error) {
            is ListForAccountError.UnknownError -> {
                manageGenericError(error.listId, error.action)
                binding.messageView.show()
            }

            is ListForAccountError.NetworkError -> {
                manageNetworkError(error.listId, error.action)
                binding.messageView.show()
            }

            else -> Unit
        }
    }

    private fun manageGenericError(listId: String, action: ListsForAccountErrorAction) {
        binding.messageView.setup(
            R.drawable.elephant_error,
            when (action) {
                ListsForAccountErrorAction.LOAD -> R.string.add_account_error_loading_list
                ListsForAccountErrorAction.ADD -> R.string.add_account_error_adding_to_list
                ListsForAccountErrorAction.DEL -> R.string.add_account_error_removing_from_list
                else -> R.string.error_generic
            },
            setupErrorMessageListener(listId, action)
        )
    }

    private fun manageNetworkError(listId: String, action: ListsForAccountErrorAction) {
        binding.messageView.setup(
            R.drawable.elephant_offline,
            when (action) {
                ListsForAccountErrorAction.LOAD -> R.string.add_account_network_error_loading_list
                ListsForAccountErrorAction.ADD -> R.string.add_account_network_error_adding_to_list
                ListsForAccountErrorAction.DEL -> R.string.add_account_network_error_removing_from_list
                else -> R.string.error_generic
            },
            setupErrorMessageListener(listId, action)
        )
    }

    private fun setupErrorMessageListener(
        listId: String,
        action: ListsForAccountErrorAction
    ): (View) -> Unit = { _: View ->
        binding.messageView.hide()
        when (action) {
            ListsForAccountErrorAction.LOAD -> viewModel.loadLists()
            ListsForAccountErrorAction.ADD -> viewModel.addAccountToList(listId)
            ListsForAccountErrorAction.DEL -> viewModel.removeAccountFromList(listId)
        }
    }

    companion object {
        private const val USER_ACCOUNT_ID = "userAccountId"

        @JvmStatic
        fun newInstance(userAccountId: String): ListsForAccountFragment {
            return ListsForAccountFragment().apply {
                arguments = bundleOf(USER_ACCOUNT_ID to userAccountId)
            }
        }
    }
}
