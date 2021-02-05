package com.ivanovsky.passnotes.presentation.core_mvvm.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.BR
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.ViewModelTypes

class ViewModelsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewTypes: ViewModelTypes
) : RecyclerView.Adapter<ViewModelsAdapter.ViewHolder>() {

    private val differ = AsyncListDiffer<BaseCellViewModel>(this, ViewModelsDiffCallback())

    fun updateItems(newItems: List<BaseCellViewModel>) {
        differ.submitList(newItems)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun getItemViewType(position: Int): Int {
        val type = differ.currentList[position]::class
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
        val viewModel = differ.currentList[position]
        if (holder.viewModel != null && holder.viewModel == viewModel) {
            return
        }

        if (holder.viewModel != null) {
            holder.binding.unbind()
        }

        holder.viewModel = viewModel
        holder.binding.setVariable(BR.viewModel, viewModel)
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

    class ViewModelsDiffCallback : DiffUtil.ItemCallback<BaseCellViewModel>() {

        override fun areItemsTheSame(
            oldItem: BaseCellViewModel,
            newItem: BaseCellViewModel
        ): Boolean =
            (oldItem.model == newItem.model)

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: BaseCellViewModel,
            newItem: BaseCellViewModel
        ): Boolean =
            (oldItem.model == newItem.model)
    }
}