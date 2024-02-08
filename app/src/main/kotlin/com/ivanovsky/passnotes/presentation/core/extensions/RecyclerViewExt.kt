package com.ivanovsky.passnotes.presentation.core.extensions

import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.adapter.ViewModelsAdapter

fun RecyclerView.setViewModels(
    viewModels: List<BaseCellViewModel>
) {
    (adapter as ViewModelsAdapter).updateItems(viewModels)
}