package com.ivanovsky.passnotes.domain.interactor.autofill

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.usecases.FindNoteForAutofillUseCase
import com.ivanovsky.passnotes.domain.usecases.IsDatabaseOpenedUseCase
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure

class AutofillInteractor(
    private val dbOpenedUseCase: IsDatabaseOpenedUseCase,
    private val findNoteUseCase: FindNoteForAutofillUseCase
) {

    fun isDatabaseOpened(): Boolean = dbOpenedUseCase.isDatabaseOpened()

    suspend fun findNoteForAutofill(structure: AutofillStructure): OperationResult<Note?> =
        findNoteUseCase.findNoteForAutofill(structure)
}