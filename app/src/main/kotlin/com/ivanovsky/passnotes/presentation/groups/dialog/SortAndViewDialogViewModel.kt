package com.ivanovsky.passnotes.presentation.groups.dialog

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.entity.SortType

class SortAndViewDialogViewModel(
    private val settings: Settings
) : ViewModel() {

    val isGroupsAtStartEnabled = MutableLiveData(settings.isGroupsAtStartEnabled)
    private var selectedSortType = settings.sortType
    private var selectedSortDirection = settings.sortDirection

    fun isSortTypeChecked(sortType: SortType): Boolean {
        return sortType == selectedSortType
    }

    fun isSortDirectionChecked(sortDirection: SortDirection): Boolean {
        return sortDirection == selectedSortDirection
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
}