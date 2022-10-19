package com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.binding.OnTextChangeListener
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.ExtendedTextPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.view.TextTransformationMethod
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.reset

class ExtendedTextPropertyCellViewModel(
    override val model: ExtendedTextPropertyCellModel,
    private val eventProvider: EventProvider,
    private val resourceProvider: ResourceProvider
) : BaseCellViewModel(model), PropertyViewModel {

    val primaryText = MutableLiveData(
        if (model.isCollapsed) {
            model.value
        } else {
            model.name
        }
    )

    val secondaryText = MutableLiveData(model.value)
    val isCollapsed = MutableLiveData(model.isCollapsed)
    val isProtected = MutableLiveData(model.isProtected)
    val isPrimaryVisibilityButtonVisible = MutableLiveData(model.isCollapsed && model.isProtected)
    val isSecondaryVisibilityButtonVisible = MutableLiveData(!model.isCollapsed && model.isProtected)
    val primaryError = MutableLiveData<String?>(null)
    val primaryHint = MutableLiveData(
        if (model.isCollapsed) {
            model.name
        } else {
            resourceProvider.getString(R.string.field_name)
        }
    )
    val secondaryHint = MutableLiveData(resourceProvider.getString(R.string.field_value))

    val primaryTextListener = object : OnTextChangeListener {
        override fun onTextChanged(text: String) {
            primaryError.reset()
        }
    }

    private val isTextHidden = MutableLiveData(true)
    val primaryTransformationMethod = MutableLiveData(
        obtainTextTransformationMethod(model.isProtected && model.isCollapsed)
    )

    val secondaryTransformationMethod = MutableLiveData(
        obtainTextTransformationMethod(model.isProtected)
    )

    init {
        isProtected.observeForever { isProtected ->
            isTextHidden.value = true
            secondaryTransformationMethod.value = obtainTextTransformationMethod(isProtected)
            primaryTransformationMethod.value = obtainTextTransformationMethod(isProtected && isCollapsedInternal())
            isPrimaryVisibilityButtonVisible.value = isCollapsedInternal() && isProtected
            isSecondaryVisibilityButtonVisible.value = !isCollapsedInternal() && isProtected
        }
        isCollapsed.observeForever { isCollapsed ->
            isPrimaryVisibilityButtonVisible.value = isCollapsed && isProtectedInternal()
            isSecondaryVisibilityButtonVisible.value = !isCollapsed && isProtectedInternal()
        }
    }

    override fun createProperty(): Property {
        val (name, value) = getNameAndValue()
        return Property(
            type = null,
            name = name,
            value = value,
            isProtected = isProtectedInternal()
        )
    }

    override fun isDataValid(): Boolean {
        val (name, value) = getNameAndValue()
        return value.isEmpty() || name.isNotEmpty()
    }

    override fun displayError() {
        val (name, value) = getNameAndValue()

        if (name.isEmpty() && value.isNotEmpty()) {
            primaryError.value = resourceProvider.getString(R.string.empty_field_name_message)
        }
    }

    fun onExpandButtonClicked() {
        if (!isCollapsedInternal() && !isAbleToCollapse()) {
            primaryError.value = resourceProvider.getString(R.string.empty_field_name_message)
            return
        }

        val (name, value) = getNameAndValue()

        isCollapsed.value = isCollapsedInternal().not()

        if (isCollapsedInternal()) {
            primaryText.value = value
            primaryHint.value = name
        } else {
            primaryText.value = name
            primaryHint.value = resourceProvider.getString(R.string.field_name)
            secondaryText.value = value
        }

        secondaryTransformationMethod.value = obtainTextTransformationMethod(isProtectedInternal())
        primaryTransformationMethod.value = obtainTextTransformationMethod(isProtectedInternal() && isCollapsedInternal())
    }

    fun onRemoveButtonClicked() {
        eventProvider.send((REMOVE_EVENT to model.id).toEvent())
    }

    fun onVisibilityButtonClicked() {
        isTextHidden.value = isTextHidden.value?.not()
        primaryTransformationMethod.value = obtainTextTransformationMethod(isProtectedInternal() && isCollapsedInternal())
        secondaryTransformationMethod.value = obtainTextTransformationMethod(isProtectedInternal())
    }

    @VisibleForTesting
    fun isAbleToCollapse(): Boolean {
        return isCollapsedInternal() || getPrimaryTextInternal().isNotEmpty()
    }

    @VisibleForTesting
    fun getNameAndValue(): Pair<String, String> {
        return if (isCollapsedInternal()) {
            Pair(getPrimaryHintInternal(), getPrimaryTextInternal())
        } else {
            Pair(getPrimaryTextInternal(), getSecondaryTextInternal())
        }
    }

    private fun isCollapsedInternal(): Boolean = isCollapsed.value ?: false

    private fun isProtectedInternal(): Boolean = isProtected.value ?: false

    private fun isTextHiddenInternal(): Boolean = isTextHidden.value ?: false

    private fun getPrimaryTextInternal(): String = primaryText.value?.trim() ?: EMPTY

    private fun getSecondaryTextInternal(): String = secondaryText.value?.trim() ?: EMPTY

    private fun getPrimaryHintInternal(): String = primaryHint.value ?: EMPTY

    private fun obtainTextTransformationMethod(isProtected: Boolean): TextTransformationMethod {
        return if (isProtected && isTextHiddenInternal()) {
            TextTransformationMethod.PASSWORD
        } else {
            TextTransformationMethod.PLANE_TEXT
        }
    }

    companion object {
        val REMOVE_EVENT = ExtendedTextPropertyCellModel::class.simpleName + "_removeEvent"
    }
}