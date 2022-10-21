package com.ivanovsky.passnotes.presentation.dialogs.sort_and_view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.entity.SearchType
import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.entity.SortType
import com.ivanovsky.passnotes.injection.GlobalInjector
import org.koin.core.parameter.parametersOf

class SortAndViewDialogViewModel(
    private val settings: Settings,
    private val args: SortAndViewDialogArgs
) : ViewModel() {

    private var selectedSearchType = settings.searchType
    private var selectedSortType = settings.sortType
    private var selectedSortDirection = settings.sortDirection

    val isGroupsAtStartChecked = MutableLiveData(settings.isGroupsAtStartEnabled)
    val isSearchTypeGroupVisible = (args.type == ScreenType.SEARCH_SCREEN)
    val isSortGroupsEnabled = MutableLiveData(isSortGroupsEnabledInternal())

    fun isSearchTypeChecked(searchType: SearchType): Boolean {
        return searchType == selectedSearchType
    }

    fun isSortTypeChecked(sortType: SortType): Boolean {
        return sortType == selectedSortType
    }

    fun isSortDirectionChecked(sortDirection: SortDirection): Boolean {
        return sortDirection == selectedSortDirection
    }

    fun onSearchTypeChanged(isChecked: Boolean, searchType: SearchType) {
        if (args.type != ScreenType.SEARCH_SCREEN) {
            return
        }

        if (isChecked && searchType != selectedSearchType) {
            selectedSearchType = searchType
            settings.searchType = searchType
            isSortGroupsEnabled.value = isSortGroupsEnabledInternal()
        }
    }

    fun onSortTypeChanged(isChecked: Boolean, sortType: SortType) {
        if (isChecked && sortType != selectedSortType) {
            selectedSortType = sortType
            settings.sortType = sortType
        }
    }

    fun onSortDirectionChanged(isChecked: Boolean, sortDirection: SortDirection) {
        if (isChecked && sortDirection != selectedSortDirection) {
            selectedSortDirection = sortDirection
            settings.sortDirection = sortDirection
        }
    }

    fun onGroupsAtStartChanged(isChecked: Boolean) {
        settings.isGroupsAtStartEnabled = isChecked
    }

    private fun isSortGroupsEnabledInternal(): Boolean {
        return args.type == ScreenType.GROUPS_SCREEN || selectedSearchType == SearchType.STRICT
    }

    class Factory(private val args: SortAndViewDialogArgs) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<SortAndViewDialogViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}