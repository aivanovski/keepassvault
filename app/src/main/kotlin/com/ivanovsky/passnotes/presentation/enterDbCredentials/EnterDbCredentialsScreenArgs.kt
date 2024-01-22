package com.ivanovsky.passnotes.presentation.enterDbCredentials

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import kotlinx.parcelize.Parcelize

@Parcelize
data class EnterDbCredentialsScreenArgs(
    val file: FileDescriptor
) : Parcelable