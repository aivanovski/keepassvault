package com.ivanovsky.passnotes.util

import androidx.compose.foundation.shape.RoundedCornerShape
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape

fun RoundedShape.toRoundedCornerShape(
    radiusInPixel: Float
): RoundedCornerShape {
    return when (this) {
        RoundedShape.ALL -> RoundedCornerShape(size = radiusInPixel)

        RoundedShape.BOTTOM -> RoundedCornerShape(
            bottomStart = radiusInPixel,
            bottomEnd = radiusInPixel
        )

        RoundedShape.TOP -> RoundedCornerShape(
            topStart = radiusInPixel,
            topEnd = radiusInPixel
        )

        RoundedShape.NONE -> RoundedCornerShape(size = 0f)
    }
}