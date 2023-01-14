package com.keylesspalace.tusky.components.instancemute.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.keylesspalace.tusky.components.instancemute.interfaces.InstanceActionListener
import com.keylesspalace.tusky.databinding.ItemMutedDomainBinding

class DomainMutesAdapter(private val actionListener: InstanceActionListener) : RecyclerView.Adapter<DomainMutesAdapter.ViewHolder>() {

    var instances: MutableList<String> = mutableListOf()
    var bottomLoading: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMutedDomainBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        return ViewHolder(binding, actionListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setupWithInstance(instances[position])
    }

    override fun getItemCount(): Int {
        var count = instances.size
        if (bottomLoading)
            ++count
        return count
    }

    fun addItems(newInstances: List<String>) {
        val end = instances.size
        instances.addAll(newInstances)
        notifyItemRangeInserted(end, instances.size)
    }

    fun addItem(instance: String) {
        instances.add(instance)
        notifyItemInserted(instances.size)
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < instances.size) {
            instances.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    class ViewHolder(
        private val binding: ItemMutedDomainBinding,
        private val actionListener: InstanceActionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun setupWithInstance(instance: String) {
            binding.mutedDomain.text = instance
            binding.mutedDomainUnmute.setOnClickListener {
                actionListener.mute(false, instance, adapterPosition)
            }
        }
    }
}
