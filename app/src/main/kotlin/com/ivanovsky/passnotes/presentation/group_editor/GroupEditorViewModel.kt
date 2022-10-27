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
    val screenState = MutableLiveData(ScreenState.notInitialized())
    val screenTitle = getScreenTitleInternal()
    val groupTitle = MutableLiveData(EMPTY)
    val errorText = MutableLiveData<String?>()
    val doneButtonVisibility = MutableLiveData(true)
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)

    private var group: Group? = null

    fun onScreenCreated() {
        if (args.mode == GroupEditorMode.EDIT) {
            loadData()
        } else {
            screenState.value = ScreenState.data()
        }
    }

    fun loadData() {
        val groupUid = args.groupUid ?: return

        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val getGroupResult = interactor.getGroup(groupUid)

            if (getGroupResult.isSucceededOrDeferred) {
                group = getGroupResult.obj
                groupTitle.value = getGroupResult.obj.title
                screenState.value = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(getGroupResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    fun onDoneButtonClicked() {
        when (args.mode) {
            GroupEditorMode.NEW -> createNewGroup()
            GroupEditorMode.EDIT -> updateGroup()
        }
    }

    fun navigateBack() = router.exit()

    private fun createNewGroup() {
        val parentGroupUid = args.parentGroupUid ?: return
        val title = groupTitle.value?.trim() ?: return

        if (title.isEmpty()) {
            errorText.value = resourceProvider.getString(R.string.empty_field)
            return
        }

        hideKeyboardEvent.call()
        doneButtonVisibility.value = false
        errorText.value = null
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val createResult = interactor.createNewGroup(title, parentGroupUid)

            if (createResult.isSucceededOrDeferred) {
                router.exit()
            } else {
                doneButtonVisibility.value = true

                val message = errorInteractor.processAndGetMessage(createResult.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    private fun updateGroup() {
        val group = group ?: return
        val newTitle = groupTitle.value?.trim() ?: return

        if (newTitle.isEmpty()) {
            errorText.value = resourceProvider.getString(R.string.empty_field)
            return
        }

        if (newTitle == group.title) {
            router.exit()
            return
        }

        hideKeyboardEvent.call()
        doneButtonVisibility.value = false
        screenState.value = ScreenState.loading()

        val newGroup = GroupEntity(
            uid = group.uid,
            title = newTitle
        )

        viewModelScope.launch {
            val updateResult = interactor.updateGroup(newGroup)

            if (updateResult.isSucceededOrDeferred) {
                router.exit()
            } else {
                doneButtonVisibility.value = true

                val message = errorInteractor.processAndGetMessage(updateResult.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    private fun getScreenTitleInternal(): String {
        return when (args.mode) {
            GroupEditorMode.NEW -> resourceProvider.getString(R.string.new_group)
            GroupEditorMode.EDIT -> resourceProvider.getString(R.string.edit_group)
        }
    }

    class Factory(private val args: GroupEditorArgs) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<GroupEditorViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}