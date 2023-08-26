package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class Template(
    val uid: UUID,
    val title: String,
    val fields: List<TemplateField>
) : Parcelable