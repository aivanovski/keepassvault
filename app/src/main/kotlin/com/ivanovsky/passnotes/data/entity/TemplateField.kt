package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TemplateField(
    val title: String,
    val position: Int?,
    val type: TemplateFieldType?
) : Parcelable