package com.ivanovsky.passnotes.presentation.core.compose.cells.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.SecondaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ThemedPreview

@Composable
fun InfoCell(
    text: String,
    @DrawableRes
    iconResId: Int = R.drawable.ic_info_24dp,
    modifier: Modifier? = null
) {
    Row(
        modifier = modifier ?: Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.element_margin),
                vertical = dimensionResource(R.dimen.half_margin)
            )
    ) {
        Image(
            painter = painterResource(iconResId),
            colorFilter = ColorFilter.tint(color = AppTheme.theme.colors.primaryIcon),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )

        Text(
            text = text,
            style = SecondaryTextStyle(),
            modifier = Modifier
                .padding(start = dimensionResource(R.dimen.half_margin))
                .align(Alignment.CenterVertically)
        )
    }
}

@Preview
@Composable
fun InfoCellLightPreview() {
    ThemedPreview(theme = LightTheme) {
        InfoCell(
            text = stringResource(R.string.medium_dummy_text)
        )
    }
}

@Preview
@Composable
fun InfoCellDarkPreview() {
    ThemedPreview(theme = DarkTheme) {
        InfoCell(
            text = stringResource(R.string.medium_dummy_text)
        )
    }
}