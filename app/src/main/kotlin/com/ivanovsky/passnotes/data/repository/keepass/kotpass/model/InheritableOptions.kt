package com.ivanovsky.passnotes.data.repository.keepass.kotpass.model

import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption

data class InheritableOptions(
    val autotypeEnabled: InheritableBooleanOption,
    val searchEnabled: InheritableBooleanOption
)