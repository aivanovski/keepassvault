package com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.GroupMargin
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.SecondaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.SmallIconSize
import com.ivanovsky.passnotes.presentation.core.compose.SmallMargin
import com.ivanovsky.passnotes.presentation.core.compose.ThemedPreview
import com.ivanovsky.passnotes.presentation.core.compose.shortDummyText
import com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog.model.ReportErrorState

@Composable
fun ReportErrorDialogScreen(viewModel: ReportErrorDialogViewModel) {
    val state by viewModel.state.collectAsState()

    ReportErrorDialogScreen(
        state = state,
        onToggleState = viewModel::onToggleStateClicked,
        onCopyClick = viewModel::onCopyButtonClicked,
        onNavigateToIssuesClick = viewModel::navigateToIssues
    )
}

@Composable
private fun ReportErrorDialogScreen(
    state: ReportErrorState,
    onToggleState: () -> Unit,
    onCopyClick: () -> Unit,
    onNavigateToIssuesClick: () -> Unit
) {
    val minHeight = if (state.isStacktraceCollapsed) 100.dp else 350.dp

    Column(
        modifier = Modifier
            .defaultMinSize(
                minWidth = dimensionResource(R.dimen.min_dialog_width),
                minHeight = minHeight
            )
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = ElementMargin,
                vertical = ElementMargin
            )
    ) {
        if (state.title.isNotEmpty()) {
            Text(
                text = state.title,
                style = PrimaryTextStyle(),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        val text = if (state.isStacktraceCollapsed) {
            stringResource(R.string.show_stacktrace)
        } else {
            state.stacktrace
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = ElementMargin)
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(onClick = onToggleState)
        ) {
            Text(
                text = text,
                style = SecondaryTextStyle(
                    fontSize = AppTheme.theme.textMetrics.small
                ),
                modifier = Modifier
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GroupMargin)
        ) {
            OutlinedButton(
                onClick = onCopyClick,
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text(text = stringResource(R.string.copy))
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = SmallMargin)
                        .size(size = SmallIconSize)
                )
            }

            Spacer(
                modifier = Modifier
                    .weight(weight = 1f)
            )

            FilledTonalButton(
                onClick = onNavigateToIssuesClick,
                colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Text(text = stringResource(R.string.go_to_issues))
            }
        }
    }
}

@Preview
@Composable
fun CollapsedReportErrorDialogPreview() {
    ThemedPreview(theme = LightTheme) {
        ReportErrorDialogScreen(
            state = newState(),
            onToggleState = {},
            onCopyClick = {},
            onNavigateToIssuesClick = {}
        )
    }
}

@Preview
@Composable
fun ExpandedReportErrorDialogPreview() {
    ThemedPreview(theme = LightTheme) {
        ReportErrorDialogScreen(
            state = newState(isStacktraceCollapsed = false),
            onToggleState = {},
            onCopyClick = {},
            onNavigateToIssuesClick = {}
        )
    }
}

@Composable
private fun newState(
    message: String = shortDummyText(),
    stacktrace: String = stringResource(R.string.dummy_stacktrace),
    isStacktraceCollapsed: Boolean = true
) =
    ReportErrorState(
        title = message,
        stacktrace = stacktrace,
        isStacktraceCollapsed = isStacktraceCollapsed
    )