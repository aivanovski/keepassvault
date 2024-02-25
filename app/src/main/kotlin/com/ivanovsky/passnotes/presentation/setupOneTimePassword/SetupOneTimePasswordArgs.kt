package com.ivanovsky.passnotes.presentation.setupOneTimePassword

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SetupOneTimePasswordArgs(
    val tokenName: String? = null,
    val tokenIssuer: String? = null
) : Parcelable