package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import timber.log.Timber

class FragmentContainerFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val initialHeight = MeasureSpec.makeMeasureSpec(
            MeasureSpec.getSize(heightMeasureSpec),
            MeasureSpec.AT_MOST
        )

        Timber.d(
            "onMeasure: heightMode=%s, childCount=%s",
            initialHeight,
            childCount
        )

        super.onMeasure(widthMeasureSpec, initialHeight)

        Timber.d(
            "onMeasure: measuredWidth=%s, measuredHeight=%s, measuredState=%s",
            measuredWidth,
            measuredHeight,
            measuredState
        )
    }
}
