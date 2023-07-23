package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TestData(
    val filenamePatterns: List<String>,
    val passwords: List<String>,
    val webdavUrl: String,
    val webdavUsername: String,
    val webdavPassword: String,
    val gitUrl: String,
    val fakeFsUrl: String,
    val fakeFsUsername: String,
    val fakeFsPassword: String
) : Parcelable