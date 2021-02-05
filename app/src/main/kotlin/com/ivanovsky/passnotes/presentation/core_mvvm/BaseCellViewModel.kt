package com.ivanovsky.passnotes.presentation.core_mvvm

import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel

abstract class BaseCellViewModel(
    open val model: BaseCellModel
) : ViewModel() {

    open fun onAttach() {
    }

    open fun onDetach() {
    }
}