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
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountState
import com.keylesspalace.tusky.components.lists.account.ui.view.adapter.ListsForAccountAdapter
import com.keylesspalace.tusky.components.lists.account.ui.viewmodel.ListsForAccountViewModel
import com.keylesspalace.tusky.core.extensions.visible
import com.keylesspalace.tusky.databinding.FragmentListsForAccountBinding
import com.keylesspalace.tusky.util.visible
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

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
                ConstraintLayout.LayoutParams.WRAP_CONTENT
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
            Timber.d("Account in list $listId: $accountIsIncluded")

            if (accountIsIncluded) {
                // REMOVE
            } else {
                viewModel.addAccountToList(listId)
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { state ->
                    Timber.d("State collected $state")

                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: ListsForAccountState) {
        //showProgressBar(state.isLoading)

        if (state.error != null) {
            Timber.e("Status: Error[${state.error}]")

            return
        }

        listsAccountAdapter.submitList(state.listsForAccount)
        binding.accountLists.visible()
    }

    private fun showProgressBar(isLoading: Boolean) {
        binding.progressBar.visible(isLoading)
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
