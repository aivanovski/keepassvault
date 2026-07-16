package com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.HalfMargin
import com.ivanovsky.passnotes.presentation.core.compose.HeaderTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.QuarterMargin
import com.ivanovsky.passnotes.presentation.core.compose.SecondaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.model.SelectorDialogItem
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.model.SelectorDialogState

@Composable
fun SelectorDialogScreen(
    viewModel: SelectorDialogViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SelectorDialogScreen(
        state = state,
        onItemClicked = viewModel::onItemClicked,
        onCancel = viewModel::onCancelClicked
    )
}

@Composable
private fun SelectorDialogScreen(
    state: SelectorDialogState,
    onItemClicked: (Int) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = ElementMargin,
                vertical = ElementMargin
            )
    ) {
        Text(
            text = state.title,
            style = HeaderTextStyle(),
            modifier = Modifier
                .fillMaxWidth()
        )

        if (state.description != null) {
            Text(
                text = state.description,
                style = SecondaryTextStyle(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = QuarterMargin)
            )
        }

        Spacer(Modifier.height(height = ElementMargin))

        state.items.forEachIndexed { index, item ->
            SelectorItem(
                item = item,
                isSelected = index == state.selectedItemIndex,
                onClick = {
                    onItemClicked(index)
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .padding(top = ElementMargin)
                .fillMaxWidth()
        ) {
            TextButton(
                colors = ButtonDefaults.textButtonColors(),
                onClick = onCancel
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun SelectorItem(
    item: SelectorDialogItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinItemHeight)
            .clickable(onClick = onClick)
            .padding(vertical = HalfMargin)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = HalfMargin)
        ) {
            Text(
                text = item.title,
                style = PrimaryTextStyle()
            )
            Text(
                text = item.description,
                style = SecondaryTextStyle(
                    fontSize = AppTheme.theme.textMetrics.secondary
                ),
                modifier = Modifier.padding(top = QuarterMargin)
            )
        }
    }
}

private val MinItemHeight = 56.dp

@Preview
@Composable
fun SelectorDialogLightPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        SelectorDialogScreen(
            state = newSelectorState(),
            onItemClicked = {},
            onCancel = {}
        )
    }
}

@Preview
@Composable
fun SelectorDialogDarkPreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        SelectorDialogScreen(
            state = newSelectorState(),
            onItemClicked = {},
            onCancel = {}
        )
    }
}

private fun newSelectorState(): SelectorDialogState {
    return SelectorDialogState(
        title = "Select sync type",
        description = "Description",
        selectedItemIndex = 1,
        items = listOf(
            SelectorDialogItem(
                title = "WebDAV",
                description = "Synchronize database with a WebDAV server"
            ),
            SelectorDialogItem(
                title = "Git",
                description = "Synchronize database through a Git repository"
            )
        )
    )
}