package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ServerCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
) : Parcelable