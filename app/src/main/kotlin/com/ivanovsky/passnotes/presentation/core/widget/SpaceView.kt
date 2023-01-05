package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View

class SpaceView(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {

    var heightInPixels: Int = 0
        set(value) {
            requestLayout()
            field = value
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(heightInPixels, MeasureSpec.EXACTLY)
        )
    }
}