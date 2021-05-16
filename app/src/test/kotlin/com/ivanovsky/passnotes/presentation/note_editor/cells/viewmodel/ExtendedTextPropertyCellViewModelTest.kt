package com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.ExtendedTextPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.view.TextTransformationMethod
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExtendedTextPropertyCellViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: ExtendedTextPropertyCellViewModel
    private lateinit var eventProvider: EventProvider
    private lateinit var resourceProvider: ResourceProvider

    @Before
    fun setup() {
        eventProvider = mockk(relaxUnitFun = true)
        resourceProvider = mockk(relaxUnitFun = true)
    }

    @Test
    fun `primaryText should be value when model collapsed`() {
        // Arrange
        val model = model(
            value = VALUE,
            isCollapsed = true
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.primaryText.value).isEqualTo(model.value)
    }

    @Test
    fun `primaryText should be name when model expanded`() {
        // Arrange
        val model = model(
            name = NAME,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.primaryText.value).isEqualTo(model.name)
    }

    @Test
    fun `secondaryText should be initialized`() {
        // Arrange
        val model = model(value = VALUE)

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.secondaryText.value).isEqualTo(model.value)
    }

    @Test
    fun `isCollapsed should be initialized`() {
        // Arrange
        val model = model(isCollapsed = true)

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.isCollapsed.value).isEqualTo(model.isCollapsed)
    }

    @Test
    fun `isProtected should be initialized`() {
        // Arrange
        val model = model(isProtected = true)

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.isProtected.value).isEqualTo(model.isProtected)
    }

    @Test
    fun `primaryError should be null`() {
        // Arrange
        val model = model()

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.primaryError.value).isNull()
    }

    @Test
    fun `primaryError should be cleared`() {
        // Arrange
        val model = model()

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        viewModel.primaryError.value = ERROR
        viewModel.primaryTextListener.onTextChanged(EMPTY)

        // Assert
        assertThat(viewModel.primaryError.value).isNull()
    }

    @Test
    fun `primaryHint should be initialized when collapsed`() {
        // Arrange
        val model = model(
            name = NAME,
            isCollapsed = true
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.primaryHint.value).isEqualTo(NAME)
    }

    @Test
    fun `primaryHint should be initialized when expanded`() {
        // Arrange
        every { resourceProvider.getString(R.string.field_name) }.returns(NAME)
        every { resourceProvider.getString(R.string.field_value) }.returns(EMPTY)
        val model = model(isCollapsed = false)

        // Act
        viewModel = viewModel(model, resourceProvider)

        // Assert
        assertThat(viewModel.primaryHint.value).isEqualTo(NAME)
    }

    @Test
    fun `secondaryHint should be initialized`() {
        // Arrange
        every { resourceProvider.getString(R.string.field_name) }.returns(EMPTY)
        every { resourceProvider.getString(R.string.field_value) }.returns(FIELD_VALUE)
        val model = model()

        // Act
        viewModel = viewModel(model, resourceProvider)

        // Assert
        assertThat(viewModel.secondaryHint.value).isEqualTo(FIELD_VALUE)
    }

    @Test
    fun `primaryTransformationMethod should be initialized when protected and collapsed`() {
        // Arrange
        val model = model(
            isProtected = true,
            isCollapsed = true
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.primaryTransformationMethod.value)
            .isEqualTo(TextTransformationMethod.PASSWORD)
    }

    @Test
    fun `primaryTransformationMethod should be initialized when not protected`() {
        // Arrange
        val model = model(
            isProtected = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.primaryTransformationMethod.value)
            .isEqualTo(TextTransformationMethod.PLANE_TEXT)
    }

    @Test
    fun `secondaryTransformationMethod should be initialized when protected`() {
        // Arrange
        val model = model(isProtected = true)

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.secondaryTransformationMethod.value)
            .isEqualTo(TextTransformationMethod.PASSWORD)
    }

    @Test
    fun `secondaryTransformationMethod should be initialized when not protected`() {
        // Arrange
        val model = model(isProtected = false)

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.secondaryTransformationMethod.value)
            .isEqualTo(TextTransformationMethod.PLANE_TEXT)
    }

    @Test
    fun `isDataValid should return true when collapsed and fields not empty`() {
        // Arrange
        val model = model(
            name = NAME,
            value = VALUE,
            isCollapsed = true
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.isDataValid()).isTrue()
    }

//    @Test
//    fun `isDataValid should return false when collapsed and primaryText empty`() {
//        // Arrange
//        val model = model(
//            value = EMPTY,
//            isCollapsed = true
//        )
//
//        // Act
//        viewModel = viewModel(model, setupResourceProvider())
//
//        // Assert
//        assertThat(viewModel.isDataValid()).isFalse()
//    }

    @Test
    fun `isDataValid should return true when expanded and fields not empty`() {
        // Arrange
        val model = model(
            name = NAME,
            value = VALUE,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.isDataValid()).isTrue()
    }

    @Test
    fun `isDataValid should return false when name is empty and value is not empty`() {
        // Arrange
        val model = model(
            name = EMPTY,
            value = VALUE,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())

        // Assert
        assertThat(viewModel.isDataValid()).isFalse()
    }

    @Test
    fun `displayError should not set error when collapsed`() {
        // Arrange
        val model = model(
            name = NAME,
            value = VALUE,
            isCollapsed = true
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        viewModel.displayError()

        // Assert
        assertThat(viewModel.primaryError.value).isNull()
    }

    @Test
    fun `displayError should not set error when expanded`() {
        // Arrange
        val model = model(
            name = NAME,
            value = VALUE,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        viewModel.displayError()

        // Assert
        assertThat(viewModel.primaryError.value).isNull()
    }

    @Test
    fun `displayError should set primaryError`() {
        // Arrange
        resourceProvider = setupResourceProvider()
        every { resourceProvider.getString(R.string.empty_field_name_message) }.returns(ERROR)
        val model = model(
            name = EMPTY,
            value = VALUE
        )

        // Act
        viewModel = viewModel(model, resourceProvider)
        viewModel.displayError()

        // Assert
        assertThat(viewModel.primaryError.value).isEqualTo(ERROR)
    }

    @Test
    fun `getNameAndValue should return name and value when collapsed`() {
        // Arrange
        val model = model(
            name = NAME,
            value = VALUE,
            isCollapsed = true
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        val (name, value) = viewModel.getNameAndValue()

        // Assert
        assertThat(name).isEqualTo(model.name)
        assertThat(value).isEqualTo(model.value)
    }

    @Test
    fun `getNameAndValue should return name and value when expanded`() {
        // Arrange
        val model = model(
            name = NAME,
            value = VALUE,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        val (name, value) = viewModel.getNameAndValue()

        // Assert
        assertThat(name).isEqualTo(model.name)
        assertThat(value).isEqualTo(model.value)
    }

    @Test
    fun `isAbleToCollapse should return true when collapsed`() {
        // Arrange
        val model = model(isCollapsed = true)

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        val isAbleToCollapse = viewModel.isAbleToCollapse()

        // Assert
        assertThat(isAbleToCollapse).isTrue()
    }

    @Test
    fun `isAbleToCollapse should return true when expanded`() {
        // Arrange
        val model = model(
            name = NAME,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        val isAbleToCollapse = viewModel.isAbleToCollapse()

        // Assert
        assertThat(isAbleToCollapse).isTrue()
    }

    @Test
    fun `isAbleToCollapse should return false when expanded`() {
        // Arrange
        val model = model(
            name = EMPTY,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        val isAbleToCollapse = viewModel.isAbleToCollapse()

        // Assert
        assertThat(isAbleToCollapse).isFalse()
    }

    @Test
    fun `createProperty should return property`() {
        // Arrange
        val model = model(
            name = NAME,
            value = VALUE,
            isProtected = true
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        val property = viewModel.createProperty()

        // Assert
        assertThat(property.type).isNull()
        assertThat(property.name).isEqualTo(model.name)
        assertThat(property.value).isEqualTo(model.value)
        assertThat(property.isProtected).isEqualTo(model.isProtected)
    }

    @Test
    fun `onExpandButtonClicked should set collapsed state`() {
        // Arrange
        val model = model(
            name = NAME,
            value = VALUE,
            isProtected = true,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        viewModel.onExpandButtonClicked()

        // Assert
        assertThat(viewModel.isCollapsed.value).isTrue()
        assertThat(viewModel.primaryHint.value).isEqualTo(model.name)
        assertThat(viewModel.primaryText.value).isEqualTo(model.value)
        assertThat(viewModel.primaryTransformationMethod.value)
            .isEqualTo(TextTransformationMethod.PASSWORD)
    }

    @Test
    fun `onExpandButtonClicked should set expanded state`() {
        // Arrange
        every { resourceProvider.getString(R.string.field_name) }.returns(FIELD_NAME)
        every { resourceProvider.getString(R.string.field_value) }.returns(EMPTY)
        val model = model(
            name = NAME,
            value = VALUE,
            isProtected = true,
            isCollapsed = true
        )

        // Act
        viewModel = viewModel(model, resourceProvider)
        viewModel.onExpandButtonClicked()

        // Assert
        assertThat(viewModel.isCollapsed.value).isFalse()
        assertThat(viewModel.primaryHint.value).isEqualTo(FIELD_NAME)
        assertThat(viewModel.primaryText.value).isEqualTo(model.name)
        assertThat(viewModel.secondaryText.value).isEqualTo(model.value)
        assertThat(viewModel.primaryTransformationMethod.value)
            .isEqualTo(TextTransformationMethod.PLANE_TEXT)
        assertThat(viewModel.secondaryTransformationMethod.value)
            .isEqualTo(TextTransformationMethod.PASSWORD)
    }

    @Test
    fun `onExpandButtonClicked should show error`() {
        // Arrange
        resourceProvider = setupResourceProvider()
        every { resourceProvider.getString(R.string.empty_field_name_message) }.returns(ERROR)
        val model = model(
            name = EMPTY,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, resourceProvider)
        viewModel.onExpandButtonClicked()

        // Assert
        assertThat(viewModel.isCollapsed.value).isFalse()
        assertThat(viewModel.primaryError.value).isEqualTo(ERROR)
    }

    @Test
    fun `secondaryTransformationMethod should be changed when isProtected changed`() {
        // Arrange
        val model = model(
            isProtected = false,
            isCollapsed = false
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        viewModel.isProtected.value = true

        // Assert
        assertThat(viewModel.secondaryTransformationMethod.value)
            .isEqualTo(TextTransformationMethod.PASSWORD)
    }

    @Test
    fun `primaryTransformationMethod should be changed when isProtected changed`() {
        // Arrange
        val model = model(
            isProtected = false,
            isCollapsed = true
        )

        // Act
        viewModel = viewModel(model, setupResourceProvider())
        viewModel.isProtected.value = true

        // Assert
        assertThat(viewModel.primaryTransformationMethod.value)
            .isEqualTo(TextTransformationMethod.PASSWORD)
    }

    @Test
    fun `onRemoveButtonClicked should send event`() {
        // Arrange
        val model = model(
            id = ID,
            isCollapsed = false
        )
        val expectedEvent = (ExtendedTextPropertyCellViewModel.REMOVE_EVENT to ID).toEvent()

        // Act
        viewModel = viewModel(model, setupResourceProvider(), eventProvider = eventProvider)
        viewModel.onRemoveButtonClicked()

        // Assert
        verify { eventProvider.send(expectedEvent) }
    }

    private fun setupResourceProvider(): ResourceProvider {
        val resourceProvider = mockk<ResourceProvider>(relaxUnitFun = true)

        every { resourceProvider.getString(R.string.field_name) }.returns(EMPTY)
        every { resourceProvider.getString(R.string.field_value) }.returns(EMPTY)

        return resourceProvider
    }

    private fun viewModel(
        model: ExtendedTextPropertyCellModel,
        resourceProvider: ResourceProvider = this.resourceProvider,
        eventProvider: EventProvider = this.eventProvider
    ) = ExtendedTextPropertyCellViewModel(
        model = model,
        eventProvider = eventProvider,
        resourceProvider = resourceProvider
    )

    private fun model(
        id: String = ID,
        name: String = EMPTY,
        value: String = EMPTY,
        isProtected: Boolean = false,
        isCollapsed: Boolean = false,
        inputType: TextInputType = TextInputType.TEXT
    ) = ExtendedTextPropertyCellModel(
        id = id,
        name = name,
        value = value,
        isProtected = isProtected,
        isCollapsed = isCollapsed,
        inputType = inputType
    )

    companion object {
        private const val EMPTY = ""
        private const val ID = "id"
        private const val NAME = "name"
        private const val VALUE = "value"
        private const val ERROR = "error"
        private const val FIELD_NAME = "field_name"
        private const val FIELD_VALUE = "field_value"
    }
}