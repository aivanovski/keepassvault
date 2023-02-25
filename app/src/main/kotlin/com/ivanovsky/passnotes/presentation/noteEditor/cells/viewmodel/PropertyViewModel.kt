package com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel

import com.ivanovsky.passnotes.data.entity.Property

interface PropertyViewModel {

    fun createProperty(): Property
    fun isDataValid(): Boolean
    fun displayError()
}