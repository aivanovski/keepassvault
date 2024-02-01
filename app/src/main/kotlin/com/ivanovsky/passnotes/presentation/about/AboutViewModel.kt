package com.ivanovsky.passnotes.presentation.about

import androidx.lifecycle.ViewModel
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.themeFlow

class AboutViewModel(
    themeProvider: ThemeProvider,
    private val router: Router
) : ViewModel() {

    val appVersion = BuildConfig.VERSION_NAME
    val appBuildType = BuildConfig.BUILD_TYPE
    val theme = themeFlow(themeProvider)

    fun onBackClicked() = router.exit()
}