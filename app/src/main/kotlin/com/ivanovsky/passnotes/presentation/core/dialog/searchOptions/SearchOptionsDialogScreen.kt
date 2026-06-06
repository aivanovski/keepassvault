package com.ivanovsky.passnotes.presentation.core.dialog.searchOptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.entity.SearchType
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.GroupMargin
import com.ivanovsky.passnotes.presentation.core.compose.HalfMargin
import com.ivanovsky.passnotes.presentation.core.compose.HeaderTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.SecondaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.SmallIconSize
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.dialog.searchOptions.model.SearchOptionState

@Composable
fun SearchOptionsDialogScreen(
    viewModel: SearchOptionsDialogViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SearchOptionsDialogScreen(
        state = state,
        onTitleChanged = viewModel::onTitleChanged,
        onUsernameChanged = viewModel::onUsernameChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onUrlChanged = viewModel::onUrlChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onOtherChanged = viewModel::onOtherChanged,
        onSearchableChanged = viewModel::onSearchableChanged,
        onRecycleBinChanged = viewModel::onRecycleBinChanged,
        onSearchTypeChanged = viewModel::onSearchTypeChanged,
        onCaseSensitiveChanged = viewModel::onCaseSensitiveChanged,
        onDoneClick = viewModel::onDoneClicked
    )
}

@Composable
private fun SearchOptionsDialogScreen(
    state: SearchOptionState,
    onTitleChanged: (Boolean) -> Unit,
    onUsernameChanged: (Boolean) -> Unit,
    onPasswordChanged: (Boolean) -> Unit,
    onUrlChanged: (Boolean) -> Unit,
    onNotesChanged: (Boolean) -> Unit,
    onOtherChanged: (Boolean) -> Unit,
    onSearchableChanged: (Boolean) -> Unit,
    onRecycleBinChanged: (Boolean) -> Unit,
    onSearchTypeChanged: (SearchType) -> Unit,
    onCaseSensitiveChanged: (Boolean) -> Unit,
    onDoneClick: () -> Unit
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
            text = stringResource(R.string.search_options),
            style = HeaderTextStyle(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        SectionTitle(
            text = stringResource(R.string.search_fields),
            modifier = Modifier.padding(top = GroupMargin)
        )
        ChipGroup {
            CheckableChip(
                text = stringResource(R.string.title),
                isChecked = state.isTitleEnabled,
                onCheckedChange = onTitleChanged
            )
            CheckableChip(
                text = stringResource(R.string.username),
                isChecked = state.isUsernameEnabled,
                onCheckedChange = onUsernameChanged
            )
            CheckableChip(
                text = stringResource(R.string.password),
                isChecked = state.isPasswordEnabled,
                onCheckedChange = onPasswordChanged
            )
            CheckableChip(
                text = stringResource(R.string.url_cap),
                isChecked = state.isUrlEnabled,
                onCheckedChange = onUrlChanged
            )
            CheckableChip(
                text = stringResource(R.string.notes),
                isChecked = state.isNotesEnabled,
                onCheckedChange = onNotesChanged
            )
            CheckableChip(
                text = stringResource(R.string.other),
                isChecked = state.isOtherEnabled,
                onCheckedChange = onOtherChanged
            )
        }

        SectionTitle(
            text = stringResource(R.string.search_scope),
            modifier = Modifier.padding(top = ElementMargin)
        )
        ChipGroup {
            CheckableChip(
                text = stringResource(R.string.searchable),
                isChecked = state.isSearchableEnabled,
                onCheckedChange = onSearchableChanged
            )
            CheckableChip(
                text = stringResource(R.string.recycle_bin),
                isChecked = state.isRecycleBinEnabled,
                onCheckedChange = onRecycleBinChanged
            )
        }

        SectionTitle(
            text = stringResource(R.string.search_type),
            modifier = Modifier.padding(top = ElementMargin)
        )
        SearchTypeGroup(
            selectedSearchType = state.searchType,
            onSearchTypeChanged = onSearchTypeChanged
        )

        SectionTitle(
            text = stringResource(R.string.additional_options),
            modifier = Modifier.padding(top = ElementMargin)
        )
        ChipGroup {
            CheckableChip(
                text = stringResource(R.string.case_sensitive),
                isChecked = state.isCaseSensitiveEnabled,
                onCheckedChange = onCaseSensitiveChanged
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = SecondaryTextStyle(),
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(HalfMargin),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun CheckableChip(
    text: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    FilterChip(
        selected = isChecked,
        onClick = {
            onCheckedChange(!isChecked)
        },
        label = {
            Text(
                text = text,
                style = PrimaryTextStyle(),
                fontSize = AppTheme.theme.textMetrics.secondary
            )
        },
        leadingIcon = if (isChecked) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SmallIconSize)
                )
            }
        } else {
            null
        }
    )
}

@Composable
private fun SearchTypeGroup(
    selectedSearchType: SearchType,
    onSearchTypeChanged: (SearchType) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(HalfMargin)
    ) {
        SearchTypeOption(
            text = stringResource(R.string.strict),
            isSelected = selectedSearchType == SearchType.STRICT,
            onClick = { onSearchTypeChanged(SearchType.STRICT) }
        )
        SearchTypeOption(
            text = stringResource(R.string.fuzzy),
            isSelected = selectedSearchType == SearchType.FUZZY,
            onClick = { onSearchTypeChanged(SearchType.FUZZY) }
        )
    }
}

@Composable
private fun SearchTypeOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(
            text = text,
            style = PrimaryTextStyle(),
            fontSize = AppTheme.theme.textMetrics.secondary,
            modifier = Modifier.padding(start = HalfMargin)
        )
    }
}

@Preview
@Composable
fun SearchOptionDialogLightPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        SearchOptionsDialogScreen(
            state = SearchOptionState(),
            onTitleChanged = {},
            onUsernameChanged = {},
            onPasswordChanged = {},
            onUrlChanged = {},
            onNotesChanged = {},
            onOtherChanged = {},
            onSearchableChanged = {},
            onRecycleBinChanged = {},
            onSearchTypeChanged = {},
            onCaseSensitiveChanged = {},
            onDoneClick = {}
        )
    }
}

@Preview
@Composable
fun SearchOptionDialogDarkPreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        SearchOptionsDialogScreen(
            state = SearchOptionState(),
            onTitleChanged = {},
            onUsernameChanged = {},
            onPasswordChanged = {},
            onUrlChanged = {},
            onNotesChanged = {},
            onOtherChanged = {},
            onSearchableChanged = {},
            onRecycleBinChanged = {},
            onSearchTypeChanged = {},
            onCaseSensitiveChanged = {},
            onDoneClick = {}
        )
    }
}