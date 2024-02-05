package com.ivanovsky.passnotes.presentation.note.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.model.HeaderCellModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape
import com.ivanovsky.passnotes.presentation.note.cells.model.AttachmentCellModel
import com.ivanovsky.passnotes.presentation.note.cells.model.NotePropertyCellModel
import com.ivanovsky.passnotes.util.StringUtils
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.formatAccordingLocale
import java.util.Date

class NoteCellModelFactory(
    private val resourceProvider: ResourceProvider,
    private val localeProvider: LocaleProvider
) {

    fun createCellModels(
        visibleIdsAndProperties: List<Pair<String, Property>>,
        idsAndAttachments: List<Pair<String, Attachment>>,
        hiddenIdsAndProperties: List<Pair<String, Property>>,
        created: Date,
        modified: Date,
        isHistoryButtonEnabled: Boolean
    ): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        for ((idx, cellIdAndProperty) in visibleIdsAndProperties.withIndex()) {
            val (cellId, property) = cellIdAndProperty

            if (idx == 0) {
                models.add(SpaceCellModel(R.dimen.half_margin))
            }

            val shape = when {
                visibleIdsAndProperties.size == 1 -> RoundedShape.ALL
                idx == 0 -> RoundedShape.TOP
                idx == visibleIdsAndProperties.lastIndex -> RoundedShape.BOTTOM
                else -> RoundedShape.NONE
            }

            models.add(
                NotePropertyCellModel(
                    id = cellId,
                    name = property.name ?: EMPTY,
                    value = property.value ?: EMPTY,
                    backgroundShape = shape,
                    backgroundColor = resourceProvider.getAttributeColor(
                        R.attr.kpSecondaryBackgroundColor
                    ),
                    isVisibilityButtonVisible = property.isProtected,
                    isValueProtected = property.isProtected
                )
            )

            if (idx < visibleIdsAndProperties.lastIndex) {
                models.add(createDividerCell())
            }
        }

        models.add(createHistoryHeaderCell())
        models.add(
            NotePropertyCellModel(
                id = "Created",
                name = "Created",
                value = created.formatAccordingLocale(localeProvider.getSystemLocale()),
                backgroundShape = RoundedShape.TOP,
                backgroundColor = resourceProvider.getAttributeColor(
                    R.attr.kpSecondaryBackgroundColor
                ),
                isVisibilityButtonVisible = false,
                isValueProtected = false
            )
        )
        models.add(createDividerCell())
        models.add(
            NotePropertyCellModel(
                id = "Modified",
                name = "Modified",
                value = modified.formatAccordingLocale(localeProvider.getSystemLocale()),
                backgroundShape = RoundedShape.BOTTOM,
                backgroundColor = resourceProvider.getAttributeColor(
                    R.attr.kpSecondaryBackgroundColor
                ),
                isVisibilityButtonVisible = false,
                isValueProtected = false
            )
        )

        if (idsAndAttachments.isNotEmpty()) {
            models.add(
                createHeaderCell(
                    title = resourceProvider.getString(R.string.attachments)
                )
            )
        }

        for ((idx, cellIdAndAttachment) in idsAndAttachments.withIndex()) {
            val (cellId, attachment) = cellIdAndAttachment

            val shape = when {
                idsAndAttachments.size == 1 -> RoundedShape.ALL
                idx == 0 -> RoundedShape.TOP
                idx == idsAndAttachments.lastIndex -> RoundedShape.BOTTOM
                else -> RoundedShape.NONE
            }

            models.add(
                AttachmentCellModel(
                    id = cellId,
                    name = attachment.name,
                    size = StringUtils.formatFileSize(attachment.data.size.toLong()),
                    backgroundShape = shape,
                    backgroundColor = resourceProvider.getAttributeColor(
                        R.attr.kpSecondaryBackgroundColor
                    )
                )
            )

            if (idx < idsAndAttachments.lastIndex) {
                models.add(createDividerCell())
            }
        }

        if (hiddenIdsAndProperties.isNotEmpty()) {
            models.add(
                createHeaderCell(
                    title = resourceProvider.getString(R.string.hidden)
                )
            )
        }

        for ((idx, cellIdAndProperty) in hiddenIdsAndProperties.withIndex()) {
            val (cellId, property) = cellIdAndProperty

            val shape = when {
                hiddenIdsAndProperties.size == 1 -> RoundedShape.ALL
                idx == 0 -> RoundedShape.TOP
                idx == hiddenIdsAndProperties.lastIndex -> RoundedShape.BOTTOM
                else -> RoundedShape.NONE
            }

            models.add(
                NotePropertyCellModel(
                    id = cellId,
                    name = property.name ?: EMPTY,
                    value = property.value ?: EMPTY,
                    backgroundShape = shape,
                    backgroundColor = resourceProvider.getAttributeColor(
                        R.attr.kpSecondaryBackgroundColor
                    ),
                    isVisibilityButtonVisible = property.isProtected,
                    isValueProtected = property.isProtected
                )
            )

            if (idx < hiddenIdsAndProperties.lastIndex) {
                models.add(createDividerCell())
            }
        }

        models.add(SpaceCellModel(R.dimen.huge_margin))

        return models
    }

    private fun createDividerCell(): DividerCellModel =
        DividerCellModel(
            color = resourceProvider.getColor(R.color.transparent),
            paddingStart = R.dimen.element_margin,
            paddingEnd = R.dimen.element_margin
        )

    private fun createHistoryHeaderCell(): HeaderCellModel =
        HeaderCellModel(
            id = null,
            title = resourceProvider.getString(R.string.history),
            description = resourceProvider.getString(R.string.previous_versions),
            isDescriptionVisible = true,
            descriptionIconResId = R.drawable.ic_chevron_right_24dp,
            color = resourceProvider.getAttributeColor(R.attr.kpSecondaryTextColor),
            isBold = false,
            isClickable = true,
            paddingHorizontal = R.dimen.double_element_margin
        )

    private fun createHeaderCell(
        title: String
    ): HeaderCellModel =
        HeaderCellModel(
            id = null,
            title = title,
            description = EMPTY,
            isDescriptionVisible = false,
            descriptionIconResId = null,
            color = resourceProvider.getAttributeColor(R.attr.kpSecondaryTextColor),
            isBold = false,
            isClickable = true,
            paddingHorizontal = R.dimen.double_element_margin
        )
}