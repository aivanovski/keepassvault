package com.ivanovsky.passnotes.presentation.group_editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.group_editor.GroupEditorInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class GroupEditorViewModel(
    private val interactor: GroupEditorInteractor,
    private val errorInteractor: ErrorInteractor,
    lockInteractor: DatabaseLockInteractor,
    private val resourceProvider: ResourceProvider,
    observerBus: ObserverBus,
    private val router: Router,
    private val args: GroupEditorArgs
) : ViewModel() {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.loading())
    val screenTitle = determineScreenTitle()
    val autotypeValues = MutableLiveData(formatAllAuotypeValues(isParentAutotypeEnabled = null))
    val selectedAutotypeValue = MutableLiveData(EMPTY)
    val title = MutableLiveData(EMPTY)
    val errorText = MutableLiveData<String?>()
    val doneButtonVisibility = MutableLiveData(true)
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)

    private var group: Group? = null
    private var parentGroup: Group? = null

    fun onScreenCreated() {
        loadData()
    }

    private fun loadData() {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val loadDataResult = interactor.loadData(
                groupUid = args.groupUid,
                parentGroupUid = args.parentGroupUid
            )
            if (loadDataResult.isFailed) {
                setScreenState(
                    ScreenState.error(
                        errorText = errorInteractor.processAndGetMessage(loadDataResult.error)
                    )
                )
                return@launch
            }

            val (group, parentGroup) = loadDataResult.obj

            onDataLoaded(group, parentGroup)
            setScreenState(ScreenState.data())
        }
    }

    fun onDoneButtonClicked() {
        when (args.mode) {
            GroupEditorMode.NEW -> createNewGroup()
            GroupEditorMode.EDIT -> updateGroup()
        }
    }

    fun navigateBack() = router.exit()

    private fun onDataLoaded(group: Group?, parentGroup: Group) {
        this.parentGroup = parentGroup
        this.group = group

        autotypeValues.value = formatAllAuotypeValues(
            isParentAutotypeEnabled = parentGroup.autotypeEnabled.isEnabled
        )

        when (args.mode) {
            GroupEditorMode.NEW -> {
                selectedAutotypeValue.value = autotypeValues.value?.firstOrNull() ?: EMPTY
            }
            GroupEditorMode.EDIT -> {
                group?.let {
                    title.value = it.title
                    selectedAutotypeValue.value = formatAutotypeValue(it.autotypeEnabled)
                }
            }
        }
    }

    private fun createNewGroup() {
        val entity = createGroupEntity()

        if (entity.title.isEmpty()) {
            errorText.value = resourceProvider.getString(R.string.empty_field)
            return
        }

        hideKeyboardEvent.call()
        doneButtonVisibility.value = false
        errorText.value = null
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val createResult = interactor.createNewGroup(entity)

            if (createResult.isSucceededOrDeferred) {
                router.exit()
            } else {
                doneButtonVisibility.value = true

                setScreenState(
                    ScreenState.dataWithError(
                        errorText = errorInteractor.processAndGetMessage(createResult.error)
                    )
                )
            }
        }
    }

    private fun updateGroup() {
        val group = group ?: return
        val newGroup = createGroupEntity()

        if (newGroup.title.isEmpty()) {
            errorText.value = resourceProvider.getString(R.string.empty_field)
            return
        }

        if (newGroup.title == group.title && newGroup.autotypeEnabled == group.autotypeEnabled) {
            router.exit()
            return
        }

        hideKeyboardEvent.call()
        doneButtonVisibility.value = false
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val updateResult = interactor.updateGroup(newGroup)

            if (updateResult.isSucceededOrDeferred) {
                router.exit()
            } else {
                doneButtonVisibility.value = true

                setScreenState(
                    ScreenState.dataWithError(
                        errorText = errorInteractor.processAndGetMessage(updateResult.error)
                    )
                )
            }
        }
    }

    private fun determineScreenTitle(): String {
        return when (args.mode) {
            GroupEditorMode.NEW -> resourceProvider.getString(R.string.new_group)
            GroupEditorMode.EDIT -> resourceProvider.getString(R.string.edit_group)
        }
    }

    private fun formatAllAuotypeValues(isParentAutotypeEnabled: Boolean?): List<String> {
        return mutableListOf<String>()
            .apply {
                if (isParentAutotypeEnabled != null) {
                    val value = if (isParentAutotypeEnabled) {
                        resourceProvider.getString(R.string.enable)
                    } else {
                        resourceProvider.getString(R.string.disable)
                    }

                    add(
                        resourceProvider.getString(
                            R.string.inherit_from_parent_with_value,
                            value
                        )
                    )
                } else {
                    add(resourceProvider.getString(R.string.inherit_from_parent))
                }
                add(resourceProvider.getString(R.string.enable))
                add(resourceProvider.getString(R.string.disable))
            }
    }

    private fun formatAutotypeValue(autotypeEnabled: InheritableBooleanOption): String {
        return when {
            autotypeEnabled.isInheritValue -> {
                val value = if (autotypeEnabled.isEnabled) {
                    resourceProvider.getString(R.string.enable)
                } else {
                    resourceProvider.getString(R.string.disable)
                }

                resourceProvider.getString(
                    R.string.inherit_from_parent_with_value,
                    value
                )
            }
            autotypeEnabled.isEnabled -> resourceProvider.getString(R.string.enable)
            else -> resourceProvider.getString(R.string.disable)
        }
    }

    private fun createGroupEntity(): GroupEntity {
        val uid = group?.uid
        val parentUid = if (args.mode == GroupEditorMode.NEW) {
            parentGroup?.uid
        } else {
            null
        }

        return GroupEntity(
            uid = uid,
            parentUid = parentUid,
            title = title.value?.trim() ?: EMPTY,
            autotypeEnabled = getAutotypeEnabledOption()
        )
    }

    private fun getAutotypeEnabledOption(): InheritableBooleanOption {
        val allValues = autotypeValues.value ?: return DEFAULT_AUTOTYPE_ENABLED_OPTION
        val selectedValue = selectedAutotypeValue.value ?: return DEFAULT_AUTOTYPE_ENABLED_OPTION
        val parentAutotypeEnabled = parentGroup?.autotypeEnabled
            ?: return DEFAULT_AUTOTYPE_ENABLED_OPTION

        return if (allValues.contains(selectedValue)) {
            val position = allValues.indexOf(selectedValue)

            val isInheritValue = (position == 0)

            InheritableBooleanOption(
                isEnabled = if (isInheritValue) {
                    parentAutotypeEnabled.isEnabled
                } else {
                    (position == 1)
                },
                isInheritValue = isInheritValue
            )
        } else {
            InheritableBooleanOption(
                isEnabled = parentAutotypeEnabled.isEnabled,
                isInheritValue = true
            )
        }
    }

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
    }

    class Factory(private val args: GroupEditorArgs) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<GroupEditorViewModel>(
                parametersOf(args)
            ) as T
        }
    }

    companion object {
        private val DEFAULT_AUTOTYPE_ENABLED_OPTION = InheritableBooleanOption(
            isEnabled = true,
            isInheritValue = true
        )
    }
}