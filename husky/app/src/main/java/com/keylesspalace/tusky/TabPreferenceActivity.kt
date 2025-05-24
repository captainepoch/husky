/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2019  Conny Duck
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

package com.keylesspalace.tusky

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import at.connyduck.sparkbutton.helpers.Utils
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.keylesspalace.tusky.adapter.ItemInteractionListener
import com.keylesspalace.tusky.adapter.ListSelectionAdapter
import com.keylesspalace.tusky.adapter.TabAdapter
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.MainTabsChangedEvent
import com.keylesspalace.tusky.core.extensions.onTextChanged
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityTabPreferenceBinding
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.visible
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from
import com.uber.autodispose.autoDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.regex.Pattern
import org.koin.android.ext.android.inject
import timber.log.Timber

class TabPreferenceActivity : BaseActivity(), ItemInteractionListener {

    private val binding by viewBinding(ActivityTabPreferenceBinding::inflate)
    private val mastodonApi: MastodonApi by inject()
    private val eventHub: EventHub by inject()
    private lateinit var currentTabs: MutableList<TabData>
    private lateinit var currentTabsAdapter: TabAdapter
    private lateinit var touchHelper: ItemTouchHelper
    private lateinit var addTabAdapter: TabAdapter
    private var tabsChanged = false
    private val selectedItemElevation by lazy { resources.getDimension(R.dimen.selected_drag_item_elevation) }

    private val hashtagRegex by lazy {
        Pattern.compile(
            "([\\w_]*[\\p{Alpha}_][\\w_]*)",
            Pattern.CASE_INSENSITIVE
        )
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
            if (binding.actionButton.isVisible) {
                finish()
            } else {
                toggleFab(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setSupportActionBar(binding.includedToolbar.toolbar)

        supportActionBar?.apply {
            setTitle(R.string.title_tab_preferences)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        currentTabs = accountManager.value.activeAccount?.tabPreferences.orEmpty().toMutableList()
        currentTabsAdapter = TabAdapter(currentTabs, false, this, currentTabs.size <= MIN_TAB_COUNT)
        binding.currentTabsRecyclerView.adapter = currentTabsAdapter
        binding.currentTabsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.currentTabsRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )

        addTabAdapter = TabAdapter(listOf(createTabDataFromId(DIRECT)), true, this)
        binding.addTabRecyclerView.adapter = addTabAdapter
        binding.addTabRecyclerView.layoutManager = LinearLayoutManager(this)

        touchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.END
                )
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return MIN_TAB_COUNT < currentTabs.size
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val temp = currentTabs[viewHolder.bindingAdapterPosition]
                currentTabs[viewHolder.bindingAdapterPosition] = currentTabs[target.bindingAdapterPosition]
                currentTabs[target.bindingAdapterPosition] = temp

                currentTabsAdapter.notifyItemMoved(
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )
                saveTabs()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                onTabRemoved(viewHolder.bindingAdapterPosition)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.elevation = selectedItemElevation
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.elevation = 0f
            }
        })

        touchHelper.attachToRecyclerView(binding.currentTabsRecyclerView)

        binding.actionButton.setOnClickListener {
            toggleFab(true)
        }

        binding.scrim.setOnClickListener {
            toggleFab(false)
        }

        binding.maxTabsInfo.text = getString(R.string.max_tab_number_reached, MAX_TAB_COUNT)

        updateAvailableTabs()

        applyForcedIntents(binding.root, null)
    }

    override fun onTabAdded(tab: TabData) {
        if (currentTabs.size >= MAX_TAB_COUNT) {
            return
        }

        toggleFab(false)

        if (tab.id == HASHTAG) {
            showAddHashtagDialog()
            return
        }

        if (tab.id == LIST) {
            showSelectListDialog()
            return
        }

        currentTabs.add(tab)
        currentTabsAdapter.notifyItemInserted(currentTabs.size - 1)
        updateAvailableTabs()
        saveTabs()
    }

    override fun onTabRemoved(position: Int) {
        currentTabs.removeAt(position)
        currentTabsAdapter.notifyItemRemoved(position)
        updateAvailableTabs()
        saveTabs()
    }

    override fun onActionChipClicked(tab: TabData, tabPosition: Int) {
        showAddHashtagDialog(tab, tabPosition)
    }

    override fun onChipClicked(tab: TabData, tabPosition: Int, chipPosition: Int) {
        val newArguments = tab.arguments.filterIndexed { i, _ -> i != chipPosition }
        val newTab = tab.copy(arguments = newArguments)
        currentTabs[tabPosition] = newTab
        saveTabs()

        currentTabsAdapter.notifyItemChanged(tabPosition)
    }

    private fun toggleFab(expand: Boolean) {
        val transition = MaterialContainerTransform().apply {
            startView = if (expand) {
                binding.actionButton
            } else {
                binding.sheet
            }
            val endView: View = if (expand) {
                binding.sheet
            } else {
                binding.actionButton
            }
            this.endView = endView
            addTarget(endView)
            scrimColor = Color.TRANSPARENT
            setPathMotion(MaterialArcMotion())
        }

        TransitionManager.beginDelayedTransition(binding.tabPreferenceContainer, transition)
        binding.actionButton.visible(!expand)
        binding.sheet.visible(expand)
        binding.scrim.visible(expand)

        onBackPressedCallback.isEnabled = expand
    }

    private fun showAddHashtagDialog(tab: TabData? = null, tabPosition: Int = 0) {
        val frameLayout = FrameLayout(this)
        val padding = Utils.dpToPx(this, 8)
        frameLayout.updatePadding(left = padding, right = padding)

        val editText = AppCompatEditText(this)
        editText.setHint(R.string.edit_hashtag_hint)
        editText.setText("")
        frameLayout.addView(editText)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_hashtag_title)
            .setView(frameLayout)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val input = editText.text.toString().trim()
                if (tab == null) {
                    val newTab = createTabDataFromId(HASHTAG, listOf(input))
                    currentTabs.add(newTab)
                    currentTabsAdapter.notifyItemInserted(currentTabs.size - 1)
                } else {
                    val newTab = tab.copy(arguments = tab.arguments + input)
                    currentTabs[tabPosition] = newTab

                    currentTabsAdapter.notifyItemChanged(tabPosition)
                }

                updateAvailableTabs()
                saveTabs()
            }.create()

        editText.onTextChanged { s, _, _, _ ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = validateHashtag(s)
        }

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = validateHashtag(editText.text)
        editText.requestFocus()
    }

    private fun showSelectListDialog() {
        val adapter = ListSelectionAdapter(this)
        mastodonApi.getLists()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(from(this, Lifecycle.Event.ON_DESTROY))
            .subscribe(
                { lists ->
                    adapter.addAll(lists)
                },
                { throwable ->
                    Timber.e("Failed to load lists", throwable)
                }
            )

        AlertDialog.Builder(this)
            .setTitle(R.string.select_list_title)
            .setAdapter(adapter) { _, position ->
                val list = adapter.getItem(position)
                val newTab = createTabDataFromId(LIST, listOf(list!!.id, list.title))
                currentTabs.add(newTab)
                currentTabsAdapter.notifyItemInserted(currentTabs.size - 1)
                updateAvailableTabs()
                saveTabs()
            }
            .show()
    }

    private fun validateHashtag(input: CharSequence?): Boolean {
        val trimmedInput = input?.trim() ?: ""
        return trimmedInput.isNotEmpty() && hashtagRegex.matcher(trimmedInput).matches()
    }

    private fun updateAvailableTabs() {
        val addableTabs: MutableList<TabData> = mutableListOf()

        val homeTab = createTabDataFromId(HOME)
        if (!currentTabs.contains(homeTab)) {
            addableTabs.add(homeTab)
        }
        val notificationTab = createTabDataFromId(NOTIFICATIONS)
        if (!currentTabs.contains(notificationTab)) {
            addableTabs.add(notificationTab)
        }
        val localTab = createTabDataFromId(LOCAL)
        if (!currentTabs.contains(localTab)) {
            addableTabs.add(localTab)
        }
        val federatedTab = createTabDataFromId(FEDERATED)
        if (!currentTabs.contains(federatedTab)) {
            addableTabs.add(federatedTab)
        }
        val bubbleTab = createTabDataFromId(BUBBLE)
        if (!currentTabs.contains(bubbleTab)) {
            addableTabs.add(bubbleTab)
        }
        val directMessagesTab = createTabDataFromId(DIRECT)
        if (!currentTabs.contains(directMessagesTab)) {
            addableTabs.add(directMessagesTab)
        }
        val chatTab = createTabDataFromId(CHATS)
        if (!currentTabs.contains(chatTab)) {
            addableTabs.add(chatTab)
        }

        addableTabs.add(createTabDataFromId(HASHTAG))
        addableTabs.add(createTabDataFromId(LIST))

        addTabAdapter.updateData(addableTabs)

        binding.maxTabsInfo.visible(addableTabs.size == 0 || currentTabs.size >= MAX_TAB_COUNT)
        currentTabsAdapter.setRemoveButtonVisible(currentTabs.size > MIN_TAB_COUNT)
    }

    override fun onStartDelete(viewHolder: RecyclerView.ViewHolder) {
        touchHelper.startSwipe(viewHolder)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        touchHelper.startDrag(viewHolder)
    }

    private fun saveTabs() {
        accountManager.value.activeAccount?.let {
            Single.fromCallable {
                it.tabPreferences = currentTabs
                accountManager.value.saveAccount(it)
            }
                .subscribeOn(Schedulers.io())
                .autoDispose(from(this, Lifecycle.Event.ON_DESTROY))
                .subscribe()
        }
        tabsChanged = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        if (tabsChanged) {
            eventHub.dispatch(MainTabsChangedEvent(currentTabs))
        }
    }

    companion object {
        private const val MIN_TAB_COUNT = 2
        private const val MAX_TAB_COUNT = 9
    }
}
