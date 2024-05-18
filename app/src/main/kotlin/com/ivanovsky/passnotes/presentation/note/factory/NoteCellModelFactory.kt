package com.ivanovsky.passnotes.presentation.note.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.DateFormatter
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.entity.Timestamp
import com.ivanovsky.passnotes.domain.otp.OtpUriFactory
import com.ivanovsky.passnotes.presentation.core.CellId
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.model.HeaderCellModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape
import com.ivanovsky.passnotes.presentation.note.cells.model.AttachmentCellModel
import com.ivanovsky.passnotes.presentation.note.cells.model.NotePropertyCellModel
import com.ivanovsky.passnotes.presentation.note.cells.model.OtpPropertyCellModel
import com.ivanovsky.passnotes.util.StringUtils
import com.ivanovsky.passnotes.util.TimeUtils.toJavaDate
import com.ivanovsky.passnotes.util.splitAt

class NoteCellModelFactory(
    private val dateFormatter: DateFormatter,
    private val resourceProvider: ResourceProvider
) {

    private val visiblePropertiesFilter = PropertyFilter.Builder()
        .visible()
        .notEmpty()
        .sortedByType()
        .build()

    fun createCellModels(
        propertiesWithIds: List<Pair<Property, CellId>>,
        attachmentsWithIds: List<Pair<Attachment, CellId>>,
        expiration: Timestamp?,
        isShowHiddenProperties: Boolean,
        isShowHistoryButton: Boolean
    ): List<BaseCellModel> {
        val allProperties = propertiesWithIds
            .map { (property, _) -> property }

        val propertyToIdMap = propertiesWithIds
            .associate { (property, id) ->
                property to id
            }

        val visibleProperties = visiblePropertiesFilter.apply(allProperties)

        val otherProperties = allProperties
            .subtract(visibleProperties.toSet())
            .toList()

        val hiddenProperties = if (isShowHiddenProperties) {
            otherProperties
        } else {
            emptyList()
        }

        val models = mutableListOf<BaseCellModel>()

        if (visibleProperties.isNotEmpty()) {
            models.add(SpaceCellModel(R.dimen.half_margin))
        }

        if (expiration != null) {
            val (beforeExpiration, afterExpiration) = splitPropertiesForExpiration(
                properties = visibleProperties
            )

            val propertyCount = beforeExpiration.size + afterExpiration.size + 1

            if (beforeExpiration.isNotEmpty()) {
                models.addAll(
                    createPropertiesCells(
                        propertiesWithIds = beforeExpiration.pairWithIds(propertyToIdMap),
                        header = null,
                        startPropertyIndex = 0,
                        propertiesInGroup = propertyCount
                    )
                )
            }

            models.add(
                createExpirationCell(
                    expiration = expiration,
                    propertyIndex = beforeExpiration.size,
                    propertiesInGroup = propertyCount
                )
            )

            if (afterExpiration.isNotEmpty()) {
                models.add(createDividerCell())

                models.addAll(
                    createPropertiesCells(
                        propertiesWithIds = afterExpiration.pairWithIds(propertyToIdMap),
                        header = null,
                        startPropertyIndex = beforeExpiration.size + 1,
                        propertiesInGroup = propertyCount
                    )
                )
            }
        } else {
            models.addAll(
                createPropertiesCells(
                    propertiesWithIds = visibleProperties.pairWithIds(propertyToIdMap),
                    header = null
                )
            )
        }

        models.addAll(createAttachmentCells(attachmentsWithIds))

        models.addAll(
            createPropertiesCells(
                propertiesWithIds = hiddenProperties.pairWithIds(propertyToIdMap),
                header = resourceProvider.getString(R.string.hidden)
            )
        )

        if (isShowHistoryButton) {
            models.add(createHistoryHeaderCell())
        }

        models.add(SpaceCellModel(R.dimen.huge_margin))

        return models
    }

    private fun List<Property>.pairWithIds(
        propertyToIdMap: Map<Property, CellId>
    ): List<Pair<Property, CellId>> {
        return this.mapNotNull { property ->
            val id = propertyToIdMap[property] ?: return@mapNotNull null

            property to id
        }
    }

    private fun createPropertiesCells(
        propertiesWithIds: List<Pair<Property, CellId>>,
        header: String?,
        startPropertyIndex: Int = 0,
        propertiesInGroup: Int = propertiesWithIds.size
    ): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        if (header != null && propertiesWithIds.isNotEmpty()) {
            models.add(
                createHeaderCell(
                    title = resourceProvider.getString(R.string.hidden)
                )
            )
        }

        for ((index, propertyWithId) in propertiesWithIds.withIndex()) {
            val (property, id) = propertyWithId

            models.add(
                createPropertyCell(
                    id = id,
                    property = property,
                    propertyIndex = index + startPropertyIndex,
                    propertyCount = propertiesInGroup
                )
            )

            if ((index + startPropertyIndex) < propertiesInGroup) {
                models.add(createDividerCell())
            }
        }

        return models
    }

    private fun createAttachmentCells(
        attachmentsWithIds: List<Pair<Attachment, CellId>>
    ): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        if (attachmentsWithIds.isNotEmpty()) {
            models.add(
                createHeaderCell(
                    title = resourceProvider.getString(R.string.attachments)
                )
            )
        }

        for ((idx, attachmentWithId) in attachmentsWithIds.withIndex()) {
            val (attachment, id) = attachmentWithId

            val shape = when {
                attachmentsWithIds.size == 1 -> RoundedShape.ALL
                idx == 0 -> RoundedShape.TOP
                idx == attachmentsWithIds.lastIndex -> RoundedShape.BOTTOM
                else -> RoundedShape.NONE
            }

            models.add(
                AttachmentCellModel(
                    id = id.value,
                    name = attachment.name,
                    size = StringUtils.formatFileSize(attachment.data.size.toLong()),
                    backgroundShape = shape,
                    backgroundColor = resourceProvider.getAttributeColor(
                        R.attr.kpSecondaryBackgroundColor
                    )
                )
            )

            if (idx < attachmentsWithIds.lastIndex) {
                models.add(createDividerCell())
            }
        }

        return models
    }

    private fun createPropertyCell(
        id: CellId,
        property: Property,
        propertyIndex: Int,
        propertyCount: Int
    ): BaseCellModel {
        val shape = when {
            propertyCount == 1 -> RoundedShape.ALL
            propertyIndex == 0 -> RoundedShape.TOP
            propertyIndex == propertyCount - 1 -> RoundedShape.BOTTOM
            else -> RoundedShape.NONE
        }

        val otpToken = if (property.type == PropertyType.OTP && property.value != null) {
            OtpUriFactory.parseUri(property.value)
        } else {
            null
        }

        return when {
            otpToken != null -> {
                OtpPropertyCellModel(
                    id = id.value,
                    title = resourceProvider.getString(R.string.one_time_password),
                    token = otpToken,
                    backgroundShape = shape,
                    backgroundColor = resourceProvider.getAttributeColor(
                        R.attr.kpSecondaryBackgroundColor
                    )
                )
            }

            else -> {
                NotePropertyCellModel(
                    id = id.value,
                    name = property.name ?: StringUtils.EMPTY,
                    value = property.value ?: StringUtils.EMPTY,
                    backgroundShape = shape,
                    backgroundColor = resourceProvider.getAttributeColor(
                        R.attr.kpSecondaryBackgroundColor
                    ),
                    isVisibilityButtonVisible = property.isProtected,
                    isValueProtected = property.isProtected,
                    iconResId = null
                )
            }
        }
    }

    private fun createExpirationCell(
        expiration: Timestamp,
        propertyIndex: Int,
        propertiesInGroup: Int
    ): BaseCellModel {
        val shape = when {
            propertiesInGroup == 1 -> RoundedShape.ALL
            propertyIndex == 0 -> RoundedShape.TOP
            propertyIndex == propertiesInGroup - 1 -> RoundedShape.BOTTOM
            else -> RoundedShape.NONE
        }

        val isExpired = (expiration.timeInMillis <= System.currentTimeMillis())

        return NotePropertyCellModel(
            id = CellIds.EXPIRATION,
            name = resourceProvider.getString(R.string.expires),
            value = dateFormatter.formatDateAndTime(expiration.toJavaDate()),
            backgroundShape = shape,
            backgroundColor = resourceProvider.getAttributeColor(
                R.attr.kpSecondaryBackgroundColor
            ),
            isVisibilityButtonVisible = false,
            isValueProtected = false,
            iconResId = if (isExpired) {
                R.drawable.ic_error_24dp
            } else {
                null
            }
        )
    }

    private fun splitPropertiesForExpiration(
        properties: List<Property>
    ): Pair<List<Property>, List<Property>> {
        val expirationIndex = determineExpirationIndex(properties)
        return if (expirationIndex != -1) {
            properties.splitAt(expirationIndex)
        } else {
            properties to emptyList()
        }
    }

    private fun determineExpirationIndex(
        properties: List<Property>
    ): Int {
        val types = properties.map { property -> property.type }
        if (PropertyType.URL in types) {
            return types.indexOf(PropertyType.URL)
        }
        if (PropertyType.NOTES in types) {
            return types.indexOf(PropertyType.NOTES)
        }

        val defaultTypeProperties = PropertyFilter.Builder()
            .filterDefaultTypes()
            .excludeByType(PropertyType.NOTES)
            .build()
            .apply(properties)

        return when {
            defaultTypeProperties.isEmpty() -> {
                0
            }

            else -> {
                defaultTypeProperties.lastIndex + 1
            }
        }
    }

    private fun createDividerCell(): DividerCellModel =
        DividerCellModel(
            color = resourceProvider.getColor(R.color.transparent),
            paddingStart = R.dimen.element_margin,
            paddingEnd = R.dimen.element_margin
        )

    private fun createHeaderCell(
        title: String
    ): HeaderCellModel =
        HeaderCellModel(
            id = null,
            title = title,
            description = StringUtils.EMPTY,
            isDescriptionVisible = false,
            descriptionIconResId = null,
            color = resourceProvider.getAttributeColor(R.attr.kpSecondaryTextColor),
            isBold = false,
            paddingHorizontal = R.dimen.double_element_margin,
            isClickable = false
        )

    private fun createHistoryHeaderCell(): HeaderCellModel =
        HeaderCellModel(
            id = null,
            title = StringUtils.EMPTY,
            description = resourceProvider.getString(R.string.history),
            isDescriptionVisible = true,
            descriptionIconResId = R.drawable.ic_chevron_right_24dp,
            color = resourceProvider.getAttributeColor(R.attr.kpSecondaryTextColor),
            isBold = false,
            isClickable = true,
            paddingHorizontal = R.dimen.double_element_margin
        )

    object CellIds {
        val EXPIRATION = "expiration"
    }
}