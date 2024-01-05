package com.ivanovsky.passnotes.presentation.autofill

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.Presentations
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.presentation.autofill.extensions.getFields
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillFieldType
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillParams
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.autofill.model.InlineSpec
import com.ivanovsky.passnotes.presentation.main.MainActivity
import com.ivanovsky.passnotes.util.StringUtils

class AutofillResponseFactory(
    private val context: Context,
    private val viewFactory: AutofillViewFactory
) {

    fun createResponseWithUnlock(
        params: AutofillParams
    ): FillResponse {
        val intent = MainActivity.createAutofillAuthenticationPendingIntent(context, params)

        val ids = params.structure.getAutofillIds().toTypedArray()

        // TODO: refactor spec retrieval
        val spec = if (Build.VERSION.SDK_INT >= 30 && params.inlineSpec is InlineSpec.InlineData) {
            params.inlineSpec.data.inlinePresentationSpecs.firstOrNull()
        } else {
            null
        }

        return when {
            Build.VERSION.SDK_INT >= 33 -> {
                val presentation = Presentations.Builder()
                    .apply {
                        if (spec != null) {
                            val inlineView = viewFactory.createUnlockInlineView(intent, spec)
                            setInlinePresentation(inlineView)
                        }

                        val view = viewFactory.createUnlockView()
                        setDialogPresentation(view)
                        setMenuPresentation(view)
                    }
                    .build()

                FillResponse.Builder()
                    .setAuthentication(
                        ids,
                        intent.intentSender,
                        presentation
                    )
                    .build()
            }

            Build.VERSION.SDK_INT >= 30 && spec != null -> {
                FillResponse.Builder()
                    .setAuthentication(
                        ids,
                        intent.intentSender,
                        viewFactory.createUnlockView(),
                        viewFactory.createUnlockInlineView(intent, spec)
                    )
                    .setSaveInfo(createSaveInfo(params.structure))
                    .build()
            }

            else -> {
                FillResponse.Builder()
                    .setAuthentication(
                        ids,
                        intent.intentSender,
                        viewFactory.createUnlockView()
                    )
                    .setSaveInfo(createSaveInfo(params.structure))
                    .build()
            }
        }
    }

    fun createResponseWithSelection(
        params: AutofillParams
    ): FillResponse {
        val intent = MainActivity.createAutofillSelectionPendingIntent(context, params)
        val ids = params.structure.getAutofillIds().toTypedArray()

        // TODO: refactor spec retrieval
        val spec = if (Build.VERSION.SDK_INT >= 30 && params.inlineSpec is InlineSpec.InlineData) {
            params.inlineSpec.data.inlinePresentationSpecs.firstOrNull()
        } else {
            null
        }

        return when {
            Build.VERSION.SDK_INT >= 33 -> {
                val presentation = Presentations.Builder()
                    .apply {
                        if (spec != null) {
                            val inlineView =
                                viewFactory.createManualSelectionInlineVies(intent, spec)
                            setInlinePresentation(inlineView)
                        }

                        val view = viewFactory.createManualSelectionView()
                        setDialogPresentation(view)
                        setMenuPresentation(view)
                    }
                    .build()

                FillResponse.Builder()
                    .setAuthentication(
                        ids,
                        intent.intentSender,
                        presentation
                    )
                    .build()
            }

            Build.VERSION.SDK_INT >= 30 && spec != null -> {
                FillResponse.Builder()
                    .setAuthentication(
                        ids,
                        intent.intentSender,
                        viewFactory.createManualSelectionView(),
                        viewFactory.createManualSelectionInlineVies(intent, spec)
                    )
                    .setSaveInfo(createSaveInfo(params.structure))
                    .build()
            }

            else -> {
                FillResponse.Builder()
                    .setAuthentication(
                        ids,
                        intent.intentSender,
                        viewFactory.createManualSelectionView()
                    )
                    .setSaveInfo(createSaveInfo(params.structure))
                    .build()
            }
        }
    }

    fun createResponseWithNoteAndSelection(
        notes: List<Note>,
        params: AutofillParams
    ): FillResponse {
        val intent = MainActivity.createAutofillSelectionPendingIntent(context, params)
        val ids = params.structure.getAutofillIds().toTypedArray()

        // TODO: refactor spec retrieval
        val spec = if (Build.VERSION.SDK_INT >= 30 && params.inlineSpec is InlineSpec.InlineData) {
            params.inlineSpec.data.inlinePresentationSpecs.firstOrNull()
        } else {
            null
        }

        return when {
            Build.VERSION.SDK_INT >= 33 -> {
                val note = notes.first()

                val title = note.title
                val description = findDescriptionForNote(note)


                val presentation = Presentations.Builder()
                    .apply {
                        if (spec != null) {
                            val inlineView = viewFactory.createEntryInlineView(
                                intent,
                                title,
                                description,
                                spec
                            )
                            setInlinePresentation(inlineView)
                        }

                        val view = viewFactory.createEntryView(note.title, description)
                        setDialogPresentation(view)
                        setMenuPresentation(view)
                    }
                    .build()
//

                val dataset = Dataset.Builder()
                    .apply {

                    }
                    .build()
//                FillResponse.Builder()
//                    .setAuthentication(
//                        ids,
//                        intent.intentSender,
//                        presentation
//                    )
//                    .build()
                FillResponse.Builder()
                    .build()
            }

            Build.VERSION.SDK_INT >= 30 && spec != null -> {
//                FillResponse.Builder()
//                    .setAuthentication(
//                        ids,
//                        intent.intentSender,
//                        viewFactory.createSelectionView(),
//                        viewFactory.createSelectionInlineVies(intent, spec)
//                    )
//                    .build()
                FillResponse.Builder()
                    .build()
            }

            else -> {
                val note = notes.first()

                FillResponse.Builder()
                    .addDataset(buildDatasetForNote(note, params.structure))
                    .addDataset(buildDatasetForSelection(intent, params.structure))
                    .build()
            }
        }
    }

    private fun buildDatasetForNote(note: Note, structure: AutofillStructure): Dataset {
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

    private fun buildDatasetForSelection(
        intent: PendingIntent,
        structure: AutofillStructure
    ): Dataset {
        val view = viewFactory.createManualSelectionView()

        return Dataset.Builder()
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

    private fun createSaveInfo(structure: AutofillStructure): SaveInfo {
        val type = structure
            .getFields()
            .mapNotNull { field ->
                when (field.type) {
                    AutofillFieldType.USERNAME -> SaveInfo.SAVE_DATA_TYPE_USERNAME
                    AutofillFieldType.PASSWORD -> SaveInfo.SAVE_DATA_TYPE_PASSWORD
                    else -> null
                }
            }
            .sum()

        val fields = structure.getAutofillIds().toTypedArray()

        return SaveInfo.Builder(type, fields)
            .build()
    }

    private fun AutofillStructure.getAutofillIds(): List<AutofillId> {
        return getFields().mapNotNull { it.autofillId }
    }

    companion object {
        private const val POSSIBLE_EMAIL_NAME = "mail"
    }
}