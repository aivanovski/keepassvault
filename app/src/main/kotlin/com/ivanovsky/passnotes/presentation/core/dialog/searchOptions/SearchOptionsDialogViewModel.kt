package com.ivanovsky.passnotes.presentation.core.dialog.searchOptions

import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.entity.SearchOptions
import com.ivanovsky.passnotes.domain.entity.SearchScope
import com.ivanovsky.passnotes.domain.entity.SearchType
import com.ivanovsky.passnotes.presentation.core.dialog.searchOptions.model.SearchOptionState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import kotlinx.coroutines.flow.MutableStateFlow

class SearchOptionsDialogViewModel(
    private val settings: Settings
) : ViewModel() {

    val state = MutableStateFlow(createInitialState())
    val dismissEvent = SingleLiveEvent<Unit>()

    fun onTitleChanged(isEnabled: Boolean) {
        state.value = state.value.copy(isTitleEnabled = isEnabled)
        saveToSettings()
    }

    fun onUsernameChanged(isEnabled: Boolean) {
        state.value = state.value.copy(isUsernameEnabled = isEnabled)
        saveToSettings()
    }

    fun onPasswordChanged(isEnabled: Boolean) {
        state.value = state.value.copy(isPasswordEnabled = isEnabled)
        saveToSettings()
    }

    fun onUrlChanged(isEnabled: Boolean) {
        state.value = state.value.copy(isUrlEnabled = isEnabled)
        saveToSettings()
    }

    fun onNotesChanged(isEnabled: Boolean) {
        state.value = state.value.copy(isNotesEnabled = isEnabled)
        saveToSettings()
    }

    fun onOtherChanged(isEnabled: Boolean) {
        state.value = state.value.copy(isOtherEnabled = isEnabled)
        saveToSettings()
    }

    fun onSearchableChanged(isEnabled: Boolean) {
        state.value = state.value.copy(isSearchableEnabled = isEnabled)
        saveToSettings()
    }

    fun onRecycleBinChanged(isEnabled: Boolean) {
        state.value = state.value.copy(isRecycleBinEnabled = isEnabled)
        saveToSettings()
    }

    fun onSearchTypeChanged(searchType: SearchType) {
        state.value = state.value.copy(searchType = searchType)
        saveToSettings()
    }

    fun onCaseSensitiveChanged(isEnabled: Boolean) {
        state.value = state.value.copy(isCaseSensitiveEnabled = isEnabled)
        saveToSettings()
    }

    fun onDoneClicked() {
        dismissEvent.call(Unit)
    }

    private fun saveToSettings() {
        settings.searchOptions = state.value.toSearchSettings()
    }

    private fun createInitialState(): SearchOptionState {
        val searchSettings = settings.searchOptions

        return SearchOptionState(
            isTitleEnabled = searchSettings.isTitleEnabled,
            isUsernameEnabled = searchSettings.isUsernameEnabled,
            isPasswordEnabled = searchSettings.isPasswordEnabled,
            isUrlEnabled = searchSettings.isUrlEnabled,
            isNotesEnabled = searchSettings.isNotesEnabled,
            isOtherEnabled = searchSettings.isOtherFieldsEnabled,
            isSearchableEnabled = searchSettings.restrictionScopes.contains(
                SearchScope.SEARCHABLE
            ),
            isRecycleBinEnabled = searchSettings.restrictionScopes.contains(
                SearchScope.RECYCLE_BIN
            ),
            searchType = searchSettings.searchType,
            isCaseSensitiveEnabled = searchSettings.isCaseSensitive
        )
    }

    private fun SearchOptionState.toSearchSettings(): SearchOptions {
        val restrictionScopes = buildSet {
            if (isSearchableEnabled) {
                add(SearchScope.SEARCHABLE)
            }
            if (isRecycleBinEnabled) {
                add(SearchScope.RECYCLE_BIN)
            }
        }

        return SearchOptions(
            searchType = searchType,
            isTitleEnabled = isTitleEnabled,
            isUsernameEnabled = isUsernameEnabled,
            isPasswordEnabled = isPasswordEnabled,
            isUrlEnabled = isUrlEnabled,
            isNotesEnabled = isNotesEnabled,
            isOtherFieldsEnabled = isOtherEnabled,
            isCaseSensitive = isCaseSensitiveEnabled,
            restrictionScopes = restrictionScopes
        )
    }
}