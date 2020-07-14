package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Template(
    val uid: UUID,
    val title: String,
    val fields: List<TemplateField>
) : Parcelable