package com.ivanovsky.passnotes.presentation.serverLogin

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.presentation.serverLogin.model.LoginType
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerLoginArgs(
    val loginType: LoginType,
    val fsAuthority: FSAuthority
) : Parcelable