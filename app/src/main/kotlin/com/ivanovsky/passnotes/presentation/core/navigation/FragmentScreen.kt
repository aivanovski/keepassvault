package com.ivanovsky.passnotes.presentation.core.navigation

import androidx.fragment.app.Fragment

interface Screen {
    fun create(): Fragment
    fun tag(): String
}