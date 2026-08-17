package com.ivanovsky.passnotes.presentation.diffViewer.cells.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.CardCornerRadius
import com.ivanovsky.passnotes.presentation.core.compose.CardElevation
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.ElementSpace
import com.ivanovsky.passnotes.presentation.core.compose.LargeCardCornerRadius
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.QuarterMargin
import com.ivanovsky.passnotes.presentation.core.compose.SecondaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ThemedPreview
import com.ivanovsky.passnotes.presentation.core.compose.cells.toId
import com.ivanovsky.passnotes.presentation.core.compose.newCellEventProvider
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.EntryDiffCellEvent
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.EntryDiffCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.EventType
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.Field
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.FieldValue
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.EntryDiffCellViewModel
import com.ivanovsky.passnotes.util.toRoundedCornerShape

@Composable
fun EntryDiffCell(
    viewModel: EntryDiffCellViewModel
) {
    val model by viewModel.observableModel.collectAsStateWithLifecycle()

    val onClick = {
        viewModel.sendEvent(
            EntryDiffCellEvent.OnExpandedChanged(model.id, !model.isExpanded)
        )
    }

    val onChecked = { isChecked: Boolean ->
        viewModel.sendEvent(
            EntryDiffCellEvent.OnSelectionChanged(model.id, isChecked)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.theme.colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
        shape = RoundedCornerShape(LargeCardCornerRadius),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ElementMargin)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = ElementMargin)
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = QuarterMargin)
            ) {
                val (path, title, checkbox) = createRefs()

                val textChain = createVerticalChain(
                    path,
                    title,
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

                Box(
                    Modifier
                        .constrainAs(checkbox) {
                            val endMargin = if (model.isCheckable) QuarterMargin else ElementMargin

                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            end.linkTo(parent.end, margin = endMargin)
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

            if (model.isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    model.fields.forEach { field ->
                        EntryField(
                            eventType = field.eventType,
                            name = field.name,
                            text = field.value.format(),
                            backgroundShape = field.backgroundShape
                        )
                    }
                }
            } else {
                val name = "${model.fields.size} fields"
                val text = model.fields.joinToString(
                    separator = ", ",
                    transform = { field -> field.name }
                )

                EntryField(
                    eventType = model.eventType,
                    name = name,
                    text = text,
                    backgroundShape = RoundedShape.ALL
                )
            }
        }
    }
}

@Composable
private fun EntryField(
    eventType: EventType,
    name: String,
    text: String,
    backgroundShape: RoundedShape
) {
    val backgroundColor = when (eventType) {
        EventType.INSERT -> AppTheme.theme.colors.diffInsert
        EventType.DELETE -> AppTheme.theme.colors.diffDelete
        EventType.UPDATE -> AppTheme.theme.colors.diffUpdate
    }

    val eventName = when (eventType) {
        EventType.INSERT -> stringResource(R.string.added)
        EventType.DELETE -> stringResource(R.string.deleted)
        EventType.UPDATE -> stringResource(R.string.changed)
    }

    Card(
        shape = backgroundShape.toRoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .padding(horizontal = ElementMargin)
    ) {
        Column(
            modifier = Modifier
                .padding(ElementMargin)
        ) {
            Row {
                Text(
                    text = name,
                    style = SecondaryTextStyle(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = dimensionResource(R.dimen.quarter_margin))
                )

                Text(
                    text = eventName,
                    style = SecondaryTextStyle(),
                    modifier = Modifier
                )
            }

            Text(
                text = text,
                style = PrimaryTextStyle(),
                modifier = Modifier
            )
        }
    }
}

private fun FieldValue.format(): String {
    return when (this) {
        is FieldValue.Value -> value
        is FieldValue.Update -> "$oldValue -> $newValue"
    }
}

@Composable
@Preview
private fun EntryDiffCellPreview() {
    ThemedPreview(theme = LightTheme) {
        Column {
            EntryDiffCell(newInsertEntryCell())
            ElementSpace()
            EntryDiffCell(newDeleteEntryCell())
            ElementSpace()
            EntryDiffCell(newUpdateEntryCell())
        }
    }
}

@Composable
fun newInsertEntryCell() =
    EntryDiffCellViewModel(
        EntryDiffCellModel(
            id = 1.toId(),
            title = "Entry: Spotify Premium",
            path = "Root / Personal / Services",
            eventType = EventType.INSERT,
            fields = listOf(
                Field(
                    eventType = EventType.INSERT,
                    name = "Title",
                    value = FieldValue.Value("Spotify Premium"),
                    backgroundShape = RoundedShape.TOP
                ),
                Field(
                    eventType = EventType.INSERT,
                    name = "Username",
                    value = FieldValue.Value("user@spotify.com"),
                    backgroundShape = RoundedShape.NONE
                ),
                Field(
                    eventType = EventType.INSERT,
                    name = "Password",
                    value = FieldValue.Value("abc123"),
                    backgroundShape = RoundedShape.NONE
                ),
                Field(
                    eventType = EventType.INSERT,
                    name = "URL",
                    value = FieldValue.Value("spotify.com"),
                    backgroundShape = RoundedShape.NONE
                )
            ),
            icon = Icons.Default.Add,
            iconBackgroundTint = AppTheme.theme.colors.diffInsert,
            accentTextColor = AppTheme.theme.colors.diffInsertText,
            isCheckable = true,
            isChecked = true,
            isExpanded = true
        ),
        eventProvider = newCellEventProvider()
    )

@Composable
fun newDeleteEntryCell() =
    EntryDiffCellViewModel(
        EntryDiffCellModel(
            id = 2.toId(),
            title = "Entry: Old Work VPN",
            path = "Root / Work",
            eventType = EventType.DELETE,
            fields = listOf(
                Field(
                    eventType = EventType.DELETE,
                    name = "Title",
                    value = FieldValue.Value("Spotify Premium"),
                    backgroundShape = RoundedShape.TOP
                ),
                Field(
                    eventType = EventType.DELETE,
                    name = "Username",
                    value = FieldValue.Value("user@spotify.com"),
                    backgroundShape = RoundedShape.BOTTOM
                )
            ),
            icon = Icons.Default.Remove,
            iconBackgroundTint = AppTheme.theme.colors.diffDelete,
            accentTextColor = AppTheme.theme.colors.diffDeleteText,
            isCheckable = false,
            isChecked = false,
            isExpanded = false
        ),
        eventProvider = newCellEventProvider()
    )

@Composable
fun newUpdateEntryCell() =
    EntryDiffCellViewModel(
        EntryDiffCellModel(
            id = 3.toId(),
            title = "Entry: Google Workspace",
            path = "Root / Personal",
            eventType = EventType.UPDATE,
            fields = listOf(
                Field(
                    eventType = EventType.INSERT,
                    name = "Password",
                    value = FieldValue.Value("password1"),
                    backgroundShape = RoundedShape.TOP
                ),
                Field(
                    eventType = EventType.DELETE,
                    name = "Notes",
                    value = FieldValue.Value("Some notes"),
                    backgroundShape = RoundedShape.NONE
                ),
                Field(
                    eventType = EventType.UPDATE,
                    name = "Username",
                    value = FieldValue.Update(
                        oldValue = "abc123",
                        newValue = "def456"
                    ),
                    backgroundShape = RoundedShape.BOTTOM
                )
            ),
            icon = Icons.Default.Edit,
            iconBackgroundTint = AppTheme.theme.colors.diffUpdate,
            accentTextColor = AppTheme.theme.colors.diffUpdateText,
            isCheckable = true,
            isChecked = true,
            isExpanded = true
        ),
        eventProvider = newCellEventProvider()
    )