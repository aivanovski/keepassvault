package com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel

import com.ivanovsky.passnotes.data.entity.Property

interface PropertyViewModel {

    fun createProperty(): Property
    fun isDataValid(): Boolean
    fun displayError()
}