package com.ivanovsky.passnotes.presentation.core.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.BR
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes

class ViewModelsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewTypes: ViewModelTypes
) : RecyclerView.Adapter<ViewModelsAdapter.ViewHolder>() {

    private val items = mutableListOf<BaseCellViewModel>()

    fun updateItems(newItems: List<BaseCellViewModel>) {
        val diffCallback = ViewModelsDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        val type = items[position]::class
        return viewTypes.getViewType(type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val layoutId = viewTypes.getLayoutResId(viewType)
        val binding: ViewDataBinding = DataBindingUtil.inflate(inflater, layoutId, parent, false)
        binding.lifecycleOwner = lifecycleOwner
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viewModel = items[position]
        if (holder.viewModel != null && holder.viewModel == viewModel) {
            return
        }

        if (holder.viewModel != null) {
            holder.binding.unbind()
        }

        holder.viewModel = viewModel
        holder.binding.setVariable(BR.viewModel, viewModel)
        holder.binding.executePendingBindings()
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.viewModel?.onAttach()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.viewModel?.onDetach()
    }

    class ViewHolder(
        val binding: ViewDataBinding,
        var viewModel: BaseCellViewModel? = null
    ) : RecyclerView.ViewHolder(binding.root)

    class ViewModelsDiffCallback(
        private val oldItems: List<BaseCellViewModel>,
        private val newItems: List<BaseCellViewModel>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]

            return if (oldItem.model.id != null && newItem.model.id != null) {
                oldItems[oldItemPosition].model.id == newItems[newItemPosition].model.id
            } else {
                oldItems[oldItemPosition].model == newItems[newItemPosition].model
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].model == newItems[newItemPosition].model
        }
    }
}