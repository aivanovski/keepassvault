package com.ivanovsky.passnotes.presentation.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.util.StringUtils.URL

@Composable
fun AboutScreen(
    version: String,
    buildType: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        val appName = stringResource(R.string.app_name)

        HeaderItem(appName)
        TextItem(stringResource(R.string.version_with_str, version))
        TextItem(stringResource(R.string.build_with_str, buildType))
        TextWithUrlItem(buildIntroText())

        HeaderItem(stringResource(R.string.about))
        TextItem(stringResource(R.string.about_licence_intro, appName))

        HeaderItem(stringResource(R.string.feedback))
        TextWithUrlItem(buildClickableUrl(stringResource(R.string.feedback_url)))

        HeaderItem(stringResource(R.string.homepage))
        TextWithUrlItem(
            text = buildClickableUrl(stringResource(R.string.homepage_url)),
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.group_margin))
        )
    }
}

@Composable
private fun HeaderItem(text: String) {
    Text(
        text = text,
        style = AppTheme.theme.typography.header,
        color = AppTheme.theme.colors.primaryText,
        modifier = Modifier
            .padding(
                start = dimensionResource(id = R.dimen.element_margin),
                end = dimensionResource(id = R.dimen.element_margin),
                top = dimensionResource(id = R.dimen.group_margin)
            )
    )
}

@Composable
private fun TextItem(text: String) {
    Text(
        text = text,
        style = AppTheme.theme.typography.primary,
        color = AppTheme.theme.colors.primaryText,
        modifier = Modifier
            .padding(
                start = dimensionResource(id = R.dimen.element_margin),
                end = dimensionResource(id = R.dimen.element_margin),
                top = dimensionResource(id = R.dimen.quarter_margin)
            )
    )
}

@Composable
private fun TextWithUrlItem(
    text: AnnotatedString,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    ClickableText(
        text = text,
        style = AppTheme.theme.typography.primary.copy(
            color = AppTheme.theme.colors.primaryText
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.element_margin),
                end = dimensionResource(id = R.dimen.element_margin),
                top = dimensionResource(id = R.dimen.quarter_margin)
            ),
        onClick = { offset ->
            val url = text.getStringAnnotations(URL, offset, offset).firstOrNull()
            if (url != null) {
                uriHandler.openUri(url.item)
            }
        }
    )
}

@Composable
private fun buildClickableUrl(url: String): AnnotatedString {
    return buildAnnotatedString {
        append(url)

        addStyle(
            style = SpanStyle(
                color = AppTheme.theme.colors.hyperlinkText,
                textDecoration = TextDecoration.Underline
            ),
            start = 0,
            end = url.length
        )

        addStringAnnotation(
            tag = URL,
            annotation = url,
            start = 0,
            end = url.length
        )
    }
}

@Composable
private fun buildIntroText(): AnnotatedString {
    return buildAnnotatedString {
        val appName = stringResource(id = R.string.app_name)
        val kotpass = stringResource(id = R.string.kotpass)
        val url = stringResource(id = R.string.kotpass_url)
        val text = stringResource(id = R.string.about_intro, appName, kotpass)

        val startIndex = text.indexOf(kotpass)
        val endIndex = startIndex + kotpass.length

        append(text)
        addStyle(
            style = SpanStyle(
                color = AppTheme.theme.colors.hyperlinkText,
                textDecoration = TextDecoration.Underline
            ),
            start = startIndex,
            end = endIndex
        )

        addStringAnnotation(
            tag = URL,
            annotation = url,
            start = startIndex,
            end = endIndex
        )
    }
}

@Preview
@Composable
fun LightPreview() {
    ThemedScreenPreview(
        theme = LightTheme
    ) {
        AboutScreen(
            version = BuildConfig.VERSION_NAME,
            buildType = BuildConfig.BUILD_TYPE
        )
    }
}

@Preview
@Composable
fun DarkPreview() {
    ThemedScreenPreview(
        theme = DarkTheme
    ) {
        AboutScreen(
            version = BuildConfig.VERSION_NAME,
            buildType = BuildConfig.BUILD_TYPE
        )
    }
}