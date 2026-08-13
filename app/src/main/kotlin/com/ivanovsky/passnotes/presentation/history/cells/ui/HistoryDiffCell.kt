package com.ivanovsky.passnotes.presentation.history.cells.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.CardCornerRadius
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.SecondaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.compose.newCellEventProvider
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffCellModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffCellViewModel
import com.ivanovsky.passnotes.util.StringUtils
import com.ivanovsky.passnotes.util.toRoundedCornerShape

@Composable
fun HistoryDiffCell(viewModel: HistoryDiffCellViewModel) {
    Card(
        shape = viewModel.model.backgroundShape.toRoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color(viewModel.model.backgroundColor)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .padding(horizontal = dimensionResource(R.dimen.element_margin))
            .clickable {
                viewModel.onClicked()
            }
    ) {
        Column(
            modifier = Modifier
                .padding(
                    start = dimensionResource(R.dimen.element_margin),
                    end = dimensionResource(R.dimen.element_margin),
                    top = dimensionResource(R.dimen.element_margin),
                    bottom = dimensionResource(R.dimen.element_margin)
                )
        ) {
            Row {
                Text(
                    text = viewModel.model.name,
                    style = SecondaryTextStyle(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = dimensionResource(R.dimen.quarter_margin))
                )

                Text(
                    text = viewModel.model.event,
                    style = SecondaryTextStyle(),
                    modifier = Modifier
                )
            }

            Text(
                text = viewModel.model.value,
                style = PrimaryTextStyle(),
                modifier = Modifier
            )
        }
    }
}

@Preview
@Composable
fun LightDiffCellPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        Column {
            HistoryDiffCell(newHistoryInsertCell())
            Spacer(modifier = Modifier.height(16.dp))
            HistoryDiffCell(newHistoryDeleteCell())
            Spacer(modifier = Modifier.height(16.dp))
            HistoryDiffCell(newHistoryUpdateCell())
        }
    }
}

@Preview
@Composable
fun DarkDiffCellPreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        Column {
            HistoryDiffCell(newHistoryInsertCell())
            Spacer(modifier = Modifier.height(16.dp))
            HistoryDiffCell(newHistoryDeleteCell())
            Spacer(modifier = Modifier.height(16.dp))
            HistoryDiffCell(newHistoryUpdateCell())
        }
    }
}

@Composable
fun newHistoryInsertCell() =
    HistoryDiffCellViewModel(
        HistoryDiffCellModel(
            id = 1,
            eventId = StringUtils.EMPTY,
            name = "UserName",
            value = "john.doe",
            event = "Added",
            backgroundShape = RoundedShape.ALL,
            backgroundColor = AppTheme.theme.colors.diffInsert.toArgb()
        ),
        newCellEventProvider()
    )

@Composable
fun newHistoryDeleteCell() =
    HistoryDiffCellViewModel(
        HistoryDiffCellModel(
            id = 2,
            eventId = StringUtils.EMPTY,
            name = "UserName",
            value = "john.doe",
            event = "Deleted",
            backgroundShape = RoundedShape.ALL,
            backgroundColor = AppTheme.theme.colors.diffDelete.toArgb()
        ),
        newCellEventProvider()
    )

@Composable
fun newHistoryUpdateCell() =
    HistoryDiffCellViewModel(
        HistoryDiffCellModel(
            id = 1,
            eventId = StringUtils.EMPTY,
            name = "UserName",
            value = "'john.doe' changed to 'john.doe@example.com'",
            event = "Changed",
            backgroundShape = RoundedShape.ALL,
            backgroundColor = AppTheme.theme.colors.diffUpdate.toArgb()
        ),
        newCellEventProvider()
    )