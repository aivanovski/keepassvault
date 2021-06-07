package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.ivanovsky.passnotes.BR
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.util.getLifecycleOwner

class CellLinearLayout(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {

    private lateinit var viewTypes: ViewModelTypes

    private var viewHolders: List<ViewHolder> = emptyList()

    fun setViewTypes(viewTypes: ViewModelTypes) {
        this.viewTypes = viewTypes
    }

    fun setViewModels(viewModels: List<BaseCellViewModel>) {
        removeAllViews()

        val lifecycleOwner = context.getLifecycleOwner() ?: throw IllegalStateException()
        val inflater = LayoutInflater.from(context)

        val pool = ViewHolderPool(viewHolders.toMutableList())
        val newViewHolders = mutableListOf<ViewHolder>()

        for (viewModel in viewModels) {
            val layoutId = viewTypes.getLayoutResId(viewModel::class)

            val binding: ViewDataBinding = if (pool.contains(layoutId)) {
                pool.take(layoutId).binding
                    .apply {
                        unbind()
                    }
            } else {
                DataBindingUtil.inflate(inflater, layoutId, this, false)
            }

            binding.also {
                it.lifecycleOwner = lifecycleOwner
                it.setVariable(BR.viewModel, viewModel)
                it.executePendingBindings()
            }

            addView(binding.root)
            newViewHolders.add(ViewHolder(layoutId, binding))
        }

        viewHolders = newViewHolders
    }

    private class ViewHolderPool(
        val viewHolders: MutableList<ViewHolder>
    ) {

        fun contains(layoutId: Int): Boolean {
            return viewHolders.any { it.layoutId == layoutId }
        }

        fun take(layoutId: Int): ViewHolder {
            val idx = viewHolders.indexOfFirst { it.layoutId == layoutId }
            return viewHolders.removeAt(idx)
        }
    }

    private class ViewHolder(
        val layoutId: Int,
        val binding: ViewDataBinding
    )
}