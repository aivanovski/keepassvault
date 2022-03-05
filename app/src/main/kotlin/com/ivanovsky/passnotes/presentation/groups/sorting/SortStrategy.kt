package com.ivanovsky.passnotes.presentation.groups.sorting

import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor

interface SortStrategy {
    fun sort(
        items: List<GroupsInteractor.Item>,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<GroupsInteractor.Item>
}