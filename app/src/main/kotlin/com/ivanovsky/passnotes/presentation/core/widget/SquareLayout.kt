package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

class SquareLayout(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val size = MeasureSpec.getSize(widthMeasureSpec)

		super.onMeasure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY))
	}
}