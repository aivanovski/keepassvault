package com.ivanovsky.passnotes.presentation.diffViewer.cells.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.ElementSpace
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.QuarterMargin
import com.ivanovsky.passnotes.presentation.core.compose.SecondaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ThemedPreview
import com.ivanovsky.passnotes.presentation.core.compose.cells.toId
import com.ivanovsky.passnotes.presentation.core.compose.newCellEventProvider
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffHeaderCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffHeaderCellViewModel

@Composable
fun DiffHeaderCell(viewModel: DiffHeaderCellViewModel) {
    val model by viewModel.observableModel.collectAsStateWithLifecycle()

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ElementMargin)
    ) {
        val (title, description, checkbox) = createRefs()

        val textChain = createVerticalChain(
            title,
            description,
            chainStyle = ChainStyle.Packed
        )

        constrain(textChain) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
        }

        Text(
            text = model.title,
            style = PrimaryTextStyle(),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                start.linkTo(parent.start)
                end.linkTo(checkbox.start, margin = ElementMargin)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            text = model.description,
            style = SecondaryTextStyle(),
            modifier = Modifier
                .constrainAs(description) {
                    start.linkTo(title.start)
                    end.linkTo(title.end)
                    width = Dimension.fillToConstraints
                }
                .padding(top = QuarterMargin)
        )

        Box(
            modifier = Modifier.constrainAs(checkbox) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }
        ) {
            if (model.isCheckable) {
                Checkbox(
                    checked = model.isChecked,
                    onCheckedChange = viewModel::onCheckedChanged,
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppTheme.theme.colors.fabColor
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun DiffHeaderCellPreview() {
    ThemedPreview(theme = LightTheme) {
        Column {
            DiffHeaderCell(
                newDiffHeaderCell(
                    title = "Local changes"
                )
            )
            ElementSpace()
            DiffHeaderCell(
                newDiffHeaderCell(
                    title = "Remote changes",
                    isCheckable = false
                )
            )
        }
    }
}

fun newDiffHeaderCell(
    title: String = "Local changes",
    description: String = "3 changes, 01.01.2024 12:00:00",
    isCheckable: Boolean = true
) = DiffHeaderCellViewModel(
    initialModel = DiffHeaderCellModel(
        id = 1.toId(),
        title = title,
        description = description,
        isCheckable = isCheckable,
        isChecked = true
    ),
    eventProvider = newCellEventProvider()
)