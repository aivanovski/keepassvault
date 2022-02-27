package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NoteCandidate(
    val properties: List<Property>
) : Parcelable