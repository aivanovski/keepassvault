package com.ivanovsky.passnotes.presentation.history.cells.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.SecondaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.compose.newEventProvider
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryHeaderCellModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryHeaderCellViewModel

@Composable
fun HistoryHeaderCell(viewModel: HistoryHeaderCellViewModel) {
    Row(
        modifier = Modifier
    ) {
        Text(
            text = viewModel.model.title,
            style = SecondaryTextStyle(),
            modifier = Modifier
                .weight(weight = 1f)
                .align(Alignment.CenterVertically)
                .padding(
                    start = dimensionResource(R.dimen.element_margin)
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable {
                    viewModel.onClicked()
                }
                .padding(
                    vertical = dimensionResource(R.dimen.half_group_margin),
                    horizontal = dimensionResource(R.dimen.element_margin)
                )

        ) {
            Text(
                text = viewModel.model.description,
                style = SecondaryTextStyle(),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )

            Image(
                painter = painterResource(viewModel.model.descriptionIcon),
                colorFilter = ColorFilter.tint(color = AppTheme.theme.colors.primaryIcon),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Preview
@Composable
fun LightDiffHeaderPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        HistoryHeaderCell(
            HistoryHeaderCellViewModel(
                model = newHistoryHeaderModel(),
                eventProvider = newEventProvider()
            )
        )
    }
}

@Preview
@Composable
fun DarkDiffHeaderPreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        HistoryHeaderCell(
            HistoryHeaderCellViewModel(
                model = newHistoryHeaderModel(),
                eventProvider = newEventProvider()
            )
        )
    }
}

fun newHistoryHeaderModel(
    title: String = "01.01.2024 12:00:00",
    description: String = "View"
) =
    HistoryHeaderCellModel(
        id = 1,
        itemId = 1,
        title = title,
        description = description,
        descriptionIcon = R.drawable.ic_chevron_right_24dp
    )