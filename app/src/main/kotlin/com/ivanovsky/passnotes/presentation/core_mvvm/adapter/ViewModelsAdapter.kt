package com.ivanovsky.passnotes.presentation.core_mvvm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.BR
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseItemViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.ViewModelTypes

class ViewModelsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewTypes: ViewModelTypes
) : RecyclerView.Adapter<ViewModelsAdapter.ViewHolder>() {

    private var items: List<BaseItemViewModel> = emptyList()

    fun updateItems(newItems: List<BaseItemViewModel>) {
        items = newItems
        notifyDataSetChanged()
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
        var viewModel: BaseItemViewModel? = null
    ) : RecyclerView.ViewHolder(binding.root)
}