package com.ivanovsky.passnotes.presentation.core

import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

abstract class BaseCellViewModel(
    open val model: BaseCellModel
) : ViewModel() {

    open fun onAttach() {
    }

    open fun onDetach() {
    }
}