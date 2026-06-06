package com.ivanovsky.passnotes.presentation.core.dialog.searchOptions.model

import com.ivanovsky.passnotes.domain.entity.SearchType

data class SearchOptionState(
    val isTitleEnabled: Boolean = true,
    val isUsernameEnabled: Boolean = true,
    val isPasswordEnabled: Boolean = false,
    val isUrlEnabled: Boolean = true,
    val isNotesEnabled: Boolean = true,
    val isOtherEnabled: Boolean = true,
    val isSearchableEnabled: Boolean = true,
    val isRecycleBinEnabled: Boolean = false,
    val searchType: SearchType = SearchType.default(),
    val isCaseSensitiveEnabled: Boolean = false
)