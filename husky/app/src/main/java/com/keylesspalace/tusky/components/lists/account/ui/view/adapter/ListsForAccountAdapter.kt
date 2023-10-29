package com.keylesspalace.tusky.components.lists.account.ui.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.components.lists.account.model.ListForAccount
import com.keylesspalace.tusky.databinding.ItemListsForAccountBinding

class ListsForAccountAdapter : ListAdapter<ListForAccount, ListsForAccountAdapter.ViewHolder>(
    ListDiffer
) {

    var onListItemClick: ((String, Boolean) -> Unit) = { _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemListsForAccountBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemListsForAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ListForAccount) {
            binding.listName.text = item.list.title
            binding.btnAddOrRemove.isChecked = item.accountIsIncluded

            binding.itemLayout.setOnClickListener {
                onListItemClick(item.list.id, item.accountIsIncluded)
            }
        }
    }

    private object ListDiffer : DiffUtil.ItemCallback<ListForAccount>() {
        override fun areItemsTheSame(
            oldItem: ListForAccount,
            newItem: ListForAccount
        ): Boolean {
            return oldItem.list.id == newItem.list.id
        }

        override fun areContentsTheSame(
            oldItem: ListForAccount,
            newItem: ListForAccount
        ): Boolean {
            return oldItem == newItem
        }
    }
}
