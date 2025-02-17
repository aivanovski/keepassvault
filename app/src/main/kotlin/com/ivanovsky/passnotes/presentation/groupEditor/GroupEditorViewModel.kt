package com.ivanovsky.passnotes.presentation.groupEditor

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
import com.ivanovsky.passnotes.domain.interactor.groupEditor.GroupEditorInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenVisibilityHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class GroupEditorViewModel(
    private val interactor: GroupEditorInteractor,
    lockInteractor: DatabaseLockInteractor,
    private val resourceProvider: ResourceProvider,
    observerBus: ObserverBus,
    private val router: Router,
    private val args: GroupEditorArgs
) : BaseScreenViewModel(
    initialState = ScreenState.loading()
) {

    val screenStateHandler = DefaultScreenVisibilityHandler()
    val screenTitle = determineScreenTitle()
    val autotypeValues = MutableLiveData(emptyList<String>())
    val selectedAutotypeValue = MutableLiveData(EMPTY)
    val searchValues = MutableLiveData(emptyList<String>())
    val selectedSearchValue = MutableLiveData(EMPTY)
    val title = MutableLiveData(EMPTY)
    val errorText = MutableLiveData<String?>()
    val doneButtonVisibility = MutableLiveData(true)
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)

    private var group: Group? = null
    private var parentGroup: Group? = null
    private var autotypeOptions = DEFAULT_OPTIONS
    private var searchOptions = DEFAULT_OPTIONS

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
                setErrorState(loadDataResult.error)
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

        autotypeOptions = createInheritableOptions(
            parentValue = parentGroup.autotypeEnabled.isEnabled
        )
        searchOptions = createInheritableOptions(
            parentValue = parentGroup.searchEnabled.isEnabled
        )

        autotypeValues.value = autotypeOptions.map { formatInheritableOption(it) }
        searchValues.value = searchOptions.map { formatInheritableOption(it) }

        when (args.mode) {
            GroupEditorMode.NEW -> {
                selectedAutotypeValue.value = autotypeValues.value?.firstOrNull() ?: EMPTY
                selectedSearchValue.value = searchValues.value?.firstOrNull() ?: EMPTY
            }
            GroupEditorMode.EDIT -> {
                group?.let {
                    title.value = it.title
                    selectedAutotypeValue.value = formatInheritableOption(it.autotypeEnabled)
                    selectedSearchValue.value = formatInheritableOption(it.searchEnabled)
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

        hideKeyboardEvent.call(Unit)
        doneButtonVisibility.value = false
        errorText.value = null
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val createResult = interactor.createNewGroup(entity)

            if (createResult.isSucceededOrDeferred) {
                router.exit()
            } else {
                doneButtonVisibility.value = true

                setErrorPanelState(createResult.error)
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

        if (newGroup.title == group.title &&
            newGroup.autotypeEnabled == group.autotypeEnabled &&
            newGroup.searchEnabled == group.searchEnabled
        ) {
            router.exit()
            return
        }

        hideKeyboardEvent.call(Unit)
        doneButtonVisibility.value = false
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val updateResult = interactor.updateGroup(newGroup)

            if (updateResult.isSucceededOrDeferred) {
                router.exit()
            } else {
                doneButtonVisibility.value = true

                setErrorPanelState(updateResult.error)
            }
        }
    }

    private fun determineScreenTitle(): String {
        return when (args.mode) {
            GroupEditorMode.NEW -> resourceProvider.getString(R.string.new_group)
            GroupEditorMode.EDIT -> resourceProvider.getString(R.string.edit_group)
        }
    }

    private fun formatInheritableOption(option: InheritableBooleanOption): String {
        return when {
            option.isInheritValue -> {
                val value = if (option.isEnabled) {
                    resourceProvider.getString(R.string.enable)
                } else {
                    resourceProvider.getString(R.string.disable)
                }

                resourceProvider.getString(
                    R.string.inherit_from_parent_with_value,
                    value
                )
            }
            option.isEnabled -> resourceProvider.getString(R.string.enable)
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
            autotypeEnabled = getAutotypeEnabledOption(),
            searchEnabled = getSearchEnabledOption()
        )
    }

    private fun getAutotypeEnabledOption(): InheritableBooleanOption {
        val allValues = autotypeValues.value ?: return DEFAULT_INHERITABLE_OPTION
        val selectedValue = selectedAutotypeValue.value ?: return DEFAULT_INHERITABLE_OPTION

        val selectedIdx = allValues.indexOf(selectedValue)
        if (selectedIdx == -1) {
            return DEFAULT_INHERITABLE_OPTION
        }

        return autotypeOptions[selectedIdx]
    }

    private fun getSearchEnabledOption(): InheritableBooleanOption {
        val allValues = searchValues.value ?: return DEFAULT_INHERITABLE_OPTION
        val selectedValue = selectedSearchValue.value ?: return DEFAULT_INHERITABLE_OPTION

        val selectedIdx = allValues.indexOf(selectedValue)
        if (selectedIdx == -1) {
            return DEFAULT_INHERITABLE_OPTION
        }

        return searchOptions[selectedIdx]
    }

    private fun createInheritableOptions(parentValue: Boolean): List<InheritableBooleanOption> {
        return listOf(
            InheritableBooleanOption(
                isEnabled = parentValue,
                isInheritValue = true
            ),
            InheritableBooleanOption.ENABLED,
            InheritableBooleanOption.DISABLED
        )
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
        private val DEFAULT_INHERITABLE_OPTION = InheritableBooleanOption(
            isEnabled = true,
            isInheritValue = true
        )

        private val DEFAULT_OPTIONS = listOf(
            InheritableBooleanOption(
                isEnabled = true,
                isInheritValue = true
            ),
            InheritableBooleanOption.ENABLED,
            InheritableBooleanOption.DISABLED
        )
    }
}