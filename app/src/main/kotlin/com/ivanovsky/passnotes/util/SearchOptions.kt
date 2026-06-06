package com.ivanovsky.passnotes.util

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.SearchOptions

fun SearchOptions.isPropertySearchable(property: Property): Boolean {
    return when (property.type) {
        PropertyType.TITLE -> isTitleEnabled
        PropertyType.USER_NAME -> isUsernameEnabled
        PropertyType.PASSWORD -> isPasswordEnabled
        PropertyType.URL -> isUrlEnabled
        PropertyType.NOTES -> isNotesEnabled
        else -> isOtherFieldsEnabled
    }
}