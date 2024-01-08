package com.ivanovsky.passnotes.presentation.groups.model

import java.util.UUID

sealed class NavigationStackItem {

    object RootGroup : NavigationStackItem()

    data class Group(val groupUid: UUID) : NavigationStackItem()

    data class Search(val query: String, val groupUid: UUID?) : NavigationStackItem()
}