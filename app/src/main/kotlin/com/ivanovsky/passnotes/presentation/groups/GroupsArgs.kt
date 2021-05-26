package com.ivanovsky.passnotes.presentation.groups

import android.os.Parcelable
import java.util.UUID
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GroupsArgs(
    val groupUid: UUID?
) : Parcelable