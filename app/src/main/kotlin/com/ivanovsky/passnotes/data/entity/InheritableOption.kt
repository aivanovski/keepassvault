package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class InheritableOption(
    val type: InheritableOptionType,
    open val isInheritValue: Boolean
) : Parcelable

@Parcelize
data class InheritableBooleanOption(
    val isEnabled: Boolean,
    override val isInheritValue: Boolean
) : InheritableOption(
    type = InheritableOptionType.BOOLEAN,
    isInheritValue = isInheritValue
)