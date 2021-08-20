package com.ivanovsky.passnotes.presentation.about

import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.lifecycle.ViewModel
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider

class AboutViewModel(
    private val resourceProvider: ResourceProvider,
    private val router: Router
) : ViewModel() {

    val appVersion = BuildConfig.VERSION_NAME
    val appBuildType = BuildConfig.BUILD_TYPE
    val appIntro = formatAppIntro()

    fun onBackClicked() = router.exit()

    private fun formatAppIntro(): Spanned {
        return SpannableStringBuilder()
            .apply {
                append(resourceProvider.getString(R.string.app_name))
                append(" ")
                append(
                    HtmlCompat.fromHtml(
                        resourceProvider.getString(
                            R.string.about_intro
                        ),
                        FROM_HTML_MODE_LEGACY
                    )
                )
            }
    }
}