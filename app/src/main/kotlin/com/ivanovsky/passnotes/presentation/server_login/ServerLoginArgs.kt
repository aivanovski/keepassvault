package com.ivanovsky.passnotes.presentation.server_login

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FSAuthority
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ServerLoginArgs(
    val fsAuthority: FSAuthority
) : Parcelable