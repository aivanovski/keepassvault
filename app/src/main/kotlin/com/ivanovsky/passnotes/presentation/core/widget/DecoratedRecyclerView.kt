package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider

class DecoratedRecyclerView(
    context: Context,
    attrs: AttributeSet
) : RecyclerView(context, attrs) {

    private val isShowDividers: Boolean
    private val resourceProvider = ResourceProvider(context)

    init {
        val params = context.obtainStyledAttributes(attrs, R.styleable.DecoratedRecyclerView)

        isShowDividers = params.getBoolean(R.styleable.DecoratedRecyclerView_isShowDividers, false)

        val layoutManager = this.layoutManager
        if (isShowDividers && layoutManager is LinearLayoutManager) {
            val dividerColor = resourceProvider.getAttributeColor(R.attr.kpDividerColor)
            val itemDecoration = DividerItemDecoration(context, layoutManager.orientation)
            itemDecoration.setDrawable(ColorDrawable(dividerColor))
            addItemDecoration(itemDecoration)
        }

        params.recycle()
    }
}