package com.ivanovsky.passnotes.domain.entity

import java.util.UUID

data class SelectionItem(
    val uid: UUID,
    val parentUid: UUID,
    val type: SelectionItemType,
)