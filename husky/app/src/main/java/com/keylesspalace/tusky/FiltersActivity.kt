/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2019  Tusky Contributors
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

import android.os.Bundle
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityFiltersBinding
import com.keylesspalace.tusky.databinding.DialogFilterBinding
import com.keylesspalace.tusky.entity.Filter
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.hide
import com.keylesspalace.tusky.util.show
import okhttp3.ResponseBody
import org.koin.android.ext.android.inject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class FiltersActivity : BaseActivity() {

    private val api: MastodonApi by inject()
    private val eventHub: EventHub by inject()
    private val binding by viewBinding(ActivityFiltersBinding::inflate)
    private lateinit var context: String
    private lateinit var filters: MutableList<Filter>

    companion object {
        const val FILTERS_CONTEXT = "filters_context"
        const val FILTERS_TITLE = "filters_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        setupToolbarBackArrow()
        binding.addFilterButton.setOnClickListener {
            showAddFilterDialog()
        }

        title = intent.getStringExtra(FILTERS_TITLE)
        val intentContext = intent.getStringExtra(FILTERS_CONTEXT)

        if (intentContext == null) {
            Toast.makeText(
                this,
                "Error getting the correct context for the filters",
                Toast.LENGTH_LONG
            ).show()
            finish()
        } else {
            context = intentContext
            loadFilters()
        }
    }

    private fun updateFilter(filter: Filter, itemIndex: Int) {
        api.updateFilter(
            filter.id,
            filter.phrase,
            filter.context,
            filter.irreversible,
            filter.wholeWord,
            null
        ).enqueue(object : Callback<Filter> {

            override fun onFailure(call: Call<Filter>, t: Throwable) {
                Toast.makeText(
                    this@FiltersActivity,
                    "Error updating filter '${filter.phrase}'",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onResponse(call: Call<Filter>, response: Response<Filter>) {
                val updatedFilter = response.body()!!

                if (updatedFilter.context.contains(context)) {
                    filters[itemIndex] = updatedFilter
                } else {
                    filters.removeAt(itemIndex)
                }
                refreshFilterDisplay()
                eventHub.dispatch(PreferenceChangedEvent(context))
            }
        })
    }

    private fun deleteFilter(itemIndex: Int) {
        val filter = filters[itemIndex]
        if (filter.context.size == 1) {
            // This is the only context for this filter; delete it
            api.deleteFilter(filters[itemIndex].id).enqueue(object : Callback<ResponseBody> {

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(
                        this@FiltersActivity,
                        "Error removing filter '${filters[itemIndex].phrase}'",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    filters.removeAt(itemIndex)
                    refreshFilterDisplay()
                    eventHub.dispatch(PreferenceChangedEvent(context))
                }
            })
        } else {
            // Keep the filter, but remove it from this context
            val oldFilter = filters[itemIndex]
            val newFilter = Filter(
                oldFilter.id, oldFilter.phrase, oldFilter.context.filter { c -> c != context },
                oldFilter.expiresAt, oldFilter.irreversible, oldFilter.wholeWord
            )
            updateFilter(newFilter, itemIndex)
        }
    }

    private fun createFilter(phrase: String, wholeWord: Boolean) {
        api.createFilter(phrase, listOf(context), false, wholeWord, null)
            .enqueue(object : Callback<Filter> {

                override fun onResponse(call: Call<Filter>, response: Response<Filter>) {
                    val filterResponse = response.body()
                    if (response.isSuccessful && filterResponse != null) {
                        filters.add(filterResponse)
                        refreshFilterDisplay()
                        eventHub.dispatch(PreferenceChangedEvent(context))
                    } else {
                        Toast.makeText(
                            this@FiltersActivity,
                            "Error creating filter '$phrase'",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Filter>, t: Throwable) {
                    Toast.makeText(
                        this@FiltersActivity,
                        "Error creating filter '$phrase'",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showAddFilterDialog() {
        val dialogBind = DialogFilterBinding.inflate(layoutInflater)
        dialogBind.phraseWholeWord.isChecked = true
        AlertDialog.Builder(this@FiltersActivity)
            .setTitle(R.string.filter_addition_dialog_title)
            .setView(dialogBind.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                createFilter(
                    dialogBind.phraseEditText.text.toString(),
                    dialogBind.phraseWholeWord.isChecked
                )
            }
            .setNeutralButton(android.R.string.cancel, null)
            .create().show()
    }

    private fun setupEditDialogForItem(itemIndex: Int) {
        // Need to show the dialog before referencing any elements from its view
        val filter = filters[itemIndex]

        val dialogBind = DialogFilterBinding.inflate(layoutInflater)
        dialogBind.phraseEditText.setText(filter.phrase)
        dialogBind.phraseWholeWord.isChecked = filter.wholeWord
        AlertDialog.Builder(this@FiltersActivity)
            .setTitle(R.string.filter_edit_dialog_title)
            .setView(dialogBind.root)
            .setPositiveButton(R.string.filter_dialog_update_button) { _, _ ->
                val oldFilter = filters[itemIndex]
                val newFilter = Filter(
                    oldFilter.id,
                    dialogBind.phraseEditText.text.toString(),
                    oldFilter.context,
                    oldFilter.expiresAt,
                    oldFilter.irreversible,
                    dialogBind.phraseWholeWord.isChecked
                )
                updateFilter(newFilter, itemIndex)
            }
            .setNegativeButton(R.string.filter_dialog_remove_button) { _, _ ->
                deleteFilter(itemIndex)
            }
            .setNeutralButton(android.R.string.cancel, null)
            .create().show()
    }

    private fun refreshFilterDisplay() {
        binding.filtersView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            filters.map { filter -> filter.phrase }
        )
        binding.filtersView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ -> setupEditDialogForItem(position) }
    }

    private fun loadFilters() {
        binding.filterMessageView.hide()
        binding.filtersView.hide()
        binding.addFilterButton.hide()
        binding.filterProgressBar.show()

        api.getFilters().enqueue(object : Callback<List<Filter>> {

            override fun onResponse(call: Call<List<Filter>>, response: Response<List<Filter>>) {
                val filterResponse = response.body()
                if (response.isSuccessful && filterResponse != null) {
                    filters = filterResponse.filter { filter -> filter.context.contains(context) }
                        .toMutableList()
                    refreshFilterDisplay()

                    binding.filtersView.show()
                    binding.addFilterButton.show()
                    binding.filterProgressBar.hide()
                } else {
                    binding.filterProgressBar.hide()
                    binding.filterMessageView.show()
                    binding.filterMessageView.setup(
                        R.drawable.elephant_error,
                        R.string.error_generic
                    ) { loadFilters() }
                }
            }

            override fun onFailure(call: Call<List<Filter>>, t: Throwable) {
                binding.filterProgressBar.hide()
                binding.filterMessageView.show()
                if (t is IOException) {
                    binding.filterMessageView.setup(
                        R.drawable.elephant_offline,
                        R.string.error_network
                    ) { loadFilters() }
                } else {
                    binding.filterMessageView.setup(
                        R.drawable.elephant_error,
                        R.string.error_generic
                    ) { loadFilters() }
                }
            }
        })
    }

    private fun setupToolbarBackArrow() {
        setSupportActionBar(binding.includedToolbar.toolbar)
        supportActionBar?.run {
            // Back button
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    // Activate back arrow in toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
