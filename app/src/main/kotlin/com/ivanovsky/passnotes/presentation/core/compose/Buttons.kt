package com.ivanovsky.passnotes.presentation.core.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivanovsky.passnotes.R

@Composable
fun ImageButton(
    @DrawableRes
    iconResId: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(size = dimensionResource(R.dimen.borderless_icon_button_size))
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = rememberRipple(bounded = false),
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(iconResId),
            colorFilter = ColorFilter.tint(color = AppTheme.theme.colors.primaryIcon),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}

@Preview
@Composable
fun LightImageButtonPreview() {
    ThemedPreview(theme = LightTheme) {
        ImageButton(
            iconResId = R.drawable.ic_info_24dp
        )
    }
}

@Preview
@Composable
fun DarkImageButtonPreview() {
    ThemedPreview(theme = DarkTheme) {
        ImageButton(
            iconResId = R.drawable.ic_info_24dp
        )
    }
}