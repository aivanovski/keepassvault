package com.ivanovsky.passnotes.ui.core.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

class SquareLayout(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val size = MeasureSpec.getSize(widthMeasureSpec)
		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)

		super.onMeasure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY))
	}
}