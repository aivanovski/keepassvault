package com.ivanovsky.passnotes.presentation.groups.dialog

import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.entity.SortType

class SortAndViewDialogViewModel(
    private val settings: Settings
) : ViewModel() {

    private var selectedSortType = SortType.default()
    private var selectedSortDirection = SortDirection.default()

    init {
        selectedSortType = settings.sortType
        selectedSortDirection = settings.sortDirection
    }

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
}