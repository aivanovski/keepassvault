package com.ivanovsky.passnotes.presentation.group

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class GroupViewModel(
    private val interactor: GroupInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData<ScreenState>(ScreenState.notInitialized())
    val groupTitle = MutableLiveData<String>(EMPTY)
    val errorText = MutableLiveData<String?>()
    val doneButtonVisibility = MutableLiveData<Boolean>(true)
    val finishScreenEvent = SingleLiveEvent<Unit>()
    val hideKeyboardEvent = SingleLiveEvent<Unit>()

    private var parentGroupUid: UUID? = null
    private var rootGroupUid: UUID? = null

    fun start(parentGroupUid: UUID?) {
        this.parentGroupUid = parentGroupUid

        screenState.value = ScreenState.loading()
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val rootUidResult = withContext(Dispatchers.Default) {
                interactor.getRootGroupUid()
            }

            if (rootUidResult.isSucceededOrDeferred) {
                rootGroupUid = rootUidResult.obj
                screenState.value = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(rootUidResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    fun createNewGroup() {
        val parentGroupUid = getParentUid() ?: return
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
            val result = withContext(Dispatchers.Default) {
                interactor.createNewGroup(title, parentGroupUid)
            }

            if (result.isSucceeded) {
                finishScreenEvent.call()
            } else {
                doneButtonVisibility.value = true

                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    private fun getParentUid(): UUID? {
        return when {
            parentGroupUid != null -> parentGroupUid
            rootGroupUid != null -> rootGroupUid
            else -> null
        }
    }
}