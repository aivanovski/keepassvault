package com.ivanovsky.passnotes.presentation.core.widget

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.databinding.WidgetNavigationPanelViewItemBinding

class NavigationPanelAdapter(
    private val onItemClicked: (position: Int) -> Unit
) : RecyclerView.Adapter<NavigationPanelAdapter.ViewHolder>() {

    private var items = emptyList<String>()

    fun updateItems(newItems: List<String>) {
        val diffResult = DiffUtil.calculateDiff(DiffCallback(items, newItems))

        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = WidgetNavigationPanelViewItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.text.text = items[position]
        holder.binding.root.setOnClickListener {
            onItemClicked.invoke(position)
        }
    }

    class ViewHolder(
        val binding: WidgetNavigationPanelViewItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    class DiffCallback(
        private val oldItems: List<String>,
        private val newItems: List<String>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldItems[oldItemPosition] == newItems[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}