package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity

fun Group.matches(query: String): Boolean {
    return title.contains(query, ignoreCase = true)
}

fun Group.toEntity(): GroupEntity {
    return GroupEntity(
        uid = uid,
        parentUid = parentUid,
        title = title,
        autotypeEnabled = autotypeEnabled,
        searchEnabled = searchEnabled
    )
}