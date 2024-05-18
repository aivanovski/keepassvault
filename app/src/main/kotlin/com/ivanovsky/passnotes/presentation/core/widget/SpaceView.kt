package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View

class SpaceView(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {

    var heightInPixels: Int? = null
        set(value) {
            field = value
            requestLayout()
        }

    var widthInPixels: Int? = null
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthInPixels = widthInPixels
        val heightInPixels = heightInPixels

        val widthSpec = if (widthInPixels != null) {
            MeasureSpec.makeMeasureSpec(widthInPixels, MeasureSpec.EXACTLY)
        } else {
            widthMeasureSpec
        }

        val heightSpec = if (heightInPixels != null) {
            MeasureSpec.makeMeasureSpec(heightInPixels, MeasureSpec.EXACTLY)
        } else {
            heightMeasureSpec
        }

        super.onMeasure(widthSpec, heightSpec)
    }
}