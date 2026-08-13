package com.ivanovsky.passnotes.presentation.core.compose.cells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.formatReadableMessage
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.CenteredBox
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.ElementSpace
import com.ivanovsky.passnotes.presentation.core.compose.ErrorTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.ThemedPreview
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.ErrorPanelCellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.toId
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.ErrorPanelCellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.mediumDummyText
import com.ivanovsky.passnotes.presentation.core.compose.newCellEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.shortDummyText
import com.ivanovsky.passnotes.presentation.core.compose.veryLongDummyText

@Composable
fun ErrorPanelCell(viewModel: ErrorPanelCellViewModel) {
    val model = viewModel.model
    val context = LocalContext.current
    val message = remember(model.error, context) {
        val resourceProvider = ResourceProvider(context)
        model.error.formatReadableMessage(resourceProvider)
    }

    Column(
        modifier = if (model.background != null) {
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = dimensionResource(R.dimen.error_panel_min_height))
                .background(color = AppTheme.theme.colors.errorBackground)
                .padding(bottom = dimensionResource(R.dimen.half_margin))
        } else {
            Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(R.dimen.half_margin))
        }
    ) {
        if (model.isCloseButtonVisible) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = viewModel::onCloseButtonClicked,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24dp),
                        tint = AppTheme.theme.colors.errorText,
                        contentDescription = null
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(48.dp))
        }

        Text(
            text = message,
            style = ErrorTextStyle(
                fontSize = AppTheme.theme.textMetrics.primary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.group_margin))
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.element_margin),
                    top = dimensionResource(R.dimen.half_margin),
                    end = dimensionResource(R.dimen.element_margin)
                )
        ) {
            OutlinedButton(onClick = viewModel::onReportButtonClicked) {
                Text(text = stringResource(R.string.report))
            }

            Spacer(modifier = Modifier.weight(1f))

            if (model.actionId != null && !model.actionButtonText.isNullOrEmpty()) {
                Button(
                    onClick = viewModel::onActionButtonClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.theme.colors.errorText,
                        contentColor = AppTheme.theme.colors.actionBarText
                    )
                ) {
                    Text(text = model.actionButtonText)
                }
            }
        }
    }
}

@Preview
@Composable
private fun LightErrorPanelPreview() {
    ThemedPreview(theme = LightTheme) {
        CenteredBox {
            ErrorPanelCell(newErrorStateCell())
        }
    }
}

@Preview
@Composable
private fun LightErrorPanelCellPreview() {
    ThemedPreview(theme = LightTheme) {
        Column {
            ErrorPanelCell(newErrorPanelCell())
            ElementSpace()
            ErrorPanelCell(newErrorPanelCell(mediumDummyText()))
            ElementSpace()
            ErrorPanelCell(newErrorPanelCell(veryLongDummyText()))
        }
    }
}

@Preview
@Composable
private fun DarkErrorPanelCellPreview() {
    ThemedPreview(theme = DarkTheme) {
        Column {
            ErrorPanelCell(newErrorPanelCell())

            ElementSpace()

            ErrorPanelCell(
                newErrorPanelCell(
                    text = mediumDummyText(),
                    isCloseButtonVisible = false,
                    actionButtonText = null
                )
            )

            ElementSpace()

            ErrorPanelCell(newErrorPanelCell(veryLongDummyText()))
        }
    }
}

@Composable
private fun newErrorStateCell(
    text: String = mediumDummyText()
) = ErrorPanelCellViewModel(
    model = ErrorPanelCellModel(
        id = 1.toId(),
        error = OperationError.newGenericError(Throwable(text)),
        background = null,
        isCloseButtonVisible = false,
        actionId = 1,
        actionButtonText = "Retry"
    ),
    eventProvider = newCellEventProvider()
)

@Composable
private fun newErrorPanelCell(
    text: String = shortDummyText(),
    isCloseButtonVisible: Boolean = true,
    actionButtonText: String? = "Retry"
) = ErrorPanelCellViewModel(
    model = ErrorPanelCellModel(
        id = 1.toId(),
        error = OperationError.newGenericError(Throwable(text)),
        background = AppTheme.theme.colors.errorBackground,
        isCloseButtonVisible = isCloseButtonVisible,
        actionId = 1,
        actionButtonText = actionButtonText
    ),
    eventProvider = newCellEventProvider()
)