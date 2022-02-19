package com.ivanovsky.passnotes.presentation.autofill

import android.content.Context
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import androidx.annotation.RequiresApi
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.main.MainActivity
import com.ivanovsky.passnotes.util.StringUtils

@RequiresApi(26)
class AutofillResponseFactory(
    private val context: Context,
    private val viewFactory: AutofillViewFactory
) {

    fun createResponseWithUnlock(structure: AutofillStructure): FillResponse {
        val intent = MainActivity.createAutofillAuthenticationPendingIntent(context, structure)

        return FillResponse.Builder()
            .setAuthentication(
                structure.getAllAutofillIds().toTypedArray(),
                intent.intentSender,
                viewFactory.createUnlockView()
            )
            .build()
    }

    fun createResponseWithSelection(structure: AutofillStructure): FillResponse {
        val intent = MainActivity.createAutofillSelectionPendingIntent(context, structure)

        return FillResponse.Builder()
            .setAuthentication(
                structure.getAllAutofillIds().toTypedArray(),
                intent.intentSender,
                viewFactory.createSelectionView()
            )
            .build()
    }

    fun createResponseWithNote(note: Note, structure: AutofillStructure): FillResponse? {
        val dataSet = buildDatasetForNote(note, structure) ?: return null

        return FillResponse.Builder()
            .addDataset(dataSet)
            .build()
    }

    fun createResponseWithNoteAndSelection(
        note: Note,
        structure: AutofillStructure
    ): FillResponse {
        val builder = FillResponse.Builder()

        val noteDataset = buildDatasetForNote(note, structure)
        if (noteDataset != null) {
            builder.addDataset(noteDataset)
        }

        val view = viewFactory.createSelectionView()
        val intent = MainActivity.createAutofillSelectionPendingIntent(context, structure)

        val selectionDataset = Dataset.Builder()
            .apply {
                if (structure.username?.autofillId != null) {
                    setValue(structure.username.autofillId, null, view)
                }
                if (structure.password?.autofillId != null) {
                    setValue(structure.password.autofillId, null, view)
                }
                setAuthentication(intent.intentSender)
            }
            .build()

        builder.addDataset(selectionDataset)

        return builder.build()
    }

    private fun buildDatasetForNote(note: Note, structure: AutofillStructure): Dataset? {
        if (Build.VERSION.SDK_INT < 26) {
            return null
        }

        val builder = Dataset.Builder()

        val userNameProperty = PropertyFilter.filterUserName(note.properties)
        val passwordProperty = PropertyFilter.filterPassword(note.properties)

        if (structure.username?.autofillId != null && userNameProperty != null) {
            builder.setValue(
                structure.username.autofillId,
                AutofillValue.forText(userNameProperty.value),
                viewFactory.createEntryView(note.title, findDescriptionForNote(note))
            )
        }

        if (structure.password?.autofillId != null && passwordProperty != null) {
            builder.setValue(
                structure.password.autofillId,
                AutofillValue.forText(passwordProperty.value),
                viewFactory.createEntryView(note.title, findDescriptionForNote(note))
            )
        }

        return builder.build()
    }

    private fun findDescriptionForNote(note: Note): String {
        val username = PropertyFilter.filterUserName(note.properties)?.value
        if (username?.isNotEmpty() == true) {
            return username
        }

        val filter = PropertyFilter.Builder()
            .notEmpty()
            .excludeByType(PropertyType.USER_NAME)
            .build()

        val filteredProperties = filter.apply(note.properties)

        for (property in filteredProperties) {
            if (property.name?.contains(POSSIBLE_EMAIL_NAME, ignoreCase = true) == true &&
                property.value?.isNotEmpty() == true
            ) {
                return property.value
            }
        }

        return StringUtils.EMPTY
    }

    private fun AutofillStructure.getAllAutofillIds(): List<AutofillId> {
        return listOfNotNull(username?.autofillId, password?.autofillId)
    }

    companion object {
        private const val POSSIBLE_EMAIL_NAME = "mail"
    }
}