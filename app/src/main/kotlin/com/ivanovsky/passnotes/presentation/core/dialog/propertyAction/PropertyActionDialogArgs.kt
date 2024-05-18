package com.ivanovsky.passnotes.presentation.core.dialog.propertyAction

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.Property
import kotlinx.parcelize.Parcelize

@Parcelize
data class PropertyActionDialogArgs(
    val property: Property
) : Parcelable