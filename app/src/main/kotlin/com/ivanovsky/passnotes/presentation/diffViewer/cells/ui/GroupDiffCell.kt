package com.ivanovsky.passnotes.presentation.diffViewer.cells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.CardElevation
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.ElementSpace
import com.ivanovsky.passnotes.presentation.core.compose.LargeCardCornerRadius
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.QuarterMargin
import com.ivanovsky.passnotes.presentation.core.compose.SecondaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.SmallMargin
import com.ivanovsky.passnotes.presentation.core.compose.ThemedPreview
import com.ivanovsky.passnotes.presentation.core.compose.cells.toId
import com.ivanovsky.passnotes.presentation.core.compose.newCellEventProvider
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.GroupDiffCellEvent.OnSelectionChanged
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.GroupDiffCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.GroupDiffCellViewModel

@Composable
fun GroupDiffCell(viewModel: GroupDiffCellViewModel) {
    val model by viewModel.observableModel.collectAsStateWithLifecycle()

    val onChecked = { isChecked: Boolean ->
        viewModel.sendEvent(OnSelectionChanged(model.id, isChecked))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.theme.colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
            shape = RoundedCornerShape(LargeCardCornerRadius),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ElementMargin)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ElementMargin)
                ) {
                    val (path, title, description, checkbox) = createRefs()

                    val textChain = createVerticalChain(
                        path,
                        title,
                        description,
                        chainStyle = ChainStyle.Packed
                    )

                    constrain(textChain) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }

                    Text(
                        text = model.path,
                        style = SecondaryTextStyle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .constrainAs(path) {
                                start.linkTo(parent.start, margin = ElementMargin)
                                end.linkTo(checkbox.start)
                                width = Dimension.fillToConstraints
                            }
                    )

                    Text(
                        text = model.title,
                        style = PrimaryTextStyle(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .constrainAs(title) {
                                start.linkTo(path.start)
                                end.linkTo(checkbox.start)
                                width = Dimension.fillToConstraints
                            }
                            .padding(top = QuarterMargin)
                    )

                    Text(
                        text = model.description,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = AppTheme.theme.colors.secondaryText,
                        modifier = Modifier
                            .constrainAs(description) {
                                start.linkTo(path.start)
                            }
                            .padding(top = QuarterMargin)
                            .clip(RoundedCornerShape(QuarterMargin))
                            .background(model.chipBackgroundTint)
                            .padding(
                                horizontal = ElementMargin,
                                vertical = SmallMargin
                            )
                    )

                    Box(
                        Modifier
                            .constrainAs(checkbox) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                end.linkTo(parent.end, margin = QuarterMargin)
                            }
                    ) {
                        if (model.isCheckable) {
                            Checkbox(
                                checked = model.isChecked,
                                onCheckedChange = onChecked,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppTheme.theme.colors.fabColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun GroupDiffCellPreview() {
    ThemedPreview(theme = LightTheme) {
        Column {
            GroupDiffCell(newInsertGroupCell())
            ElementSpace()
            GroupDiffCell(newDeleteGroupCell())
        }
    }
}

@Composable
fun newInsertGroupCell() =
    GroupDiffCellViewModel(
        GroupDiffCellModel(
            id = 1.toId(),
            title = "Group: Social Media",
            description = stringResource(R.string.added),
            path = "Root / Personal",
            chipBackgroundTint = AppTheme.theme.colors.diffInsert,
            isCheckable = true,
            isChecked = false
        ),
        newCellEventProvider()
    )

@Composable
fun newDeleteGroupCell() =
    GroupDiffCellViewModel(
        GroupDiffCellModel(
            id = 2.toId(),
            title = "Group: Banking",
            description = stringResource(R.string.deleted),
            path = "Root / Personal",
            chipBackgroundTint = AppTheme.theme.colors.diffDelete,
            isCheckable = false,
            isChecked = false
        ),
        newCellEventProvider()
    )