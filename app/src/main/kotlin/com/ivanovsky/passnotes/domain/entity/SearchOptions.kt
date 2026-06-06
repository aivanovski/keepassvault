package com.ivanovsky.passnotes.domain.entity

data class SearchOptions(
    val searchType: SearchType,
    val isTitleEnabled: Boolean,
    val isUsernameEnabled: Boolean,
    val isPasswordEnabled: Boolean,
    val isUrlEnabled: Boolean,
    val isNotesEnabled: Boolean,
    val isOtherFieldsEnabled: Boolean,
    val isCaseSensitive: Boolean,
    val restrictionScopes: Set<SearchScope>
) {

    companion object {
        val DEFAULT = SearchOptions(
            searchType = SearchType.default(),
            isTitleEnabled = true,
            isUsernameEnabled = true,
            isPasswordEnabled = false,
            isUrlEnabled = true,
            isNotesEnabled = true,
            isOtherFieldsEnabled = true,
            isCaseSensitive = false,
            restrictionScopes = emptySet()
        )
    }
}