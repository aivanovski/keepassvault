package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.databinding.BindingAdapter
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.WidgetNavigationPanelViewBinding
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnItemClickListener

class NavigationPanelView(
    context: Context,
    attrs: AttributeSet
) : FrameLayout(context, attrs) {

    var items: List<String>? = emptyList()
        set(values) {
            field = values
            setValuesInternal(values)
        }

    private val binding: WidgetNavigationPanelViewBinding
    private val adapter: NavigationPanelAdapter
    private var onItemClicked: OnItemClickListener? = null

    init {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.widget_navigation_panel_view, this, true)
        binding = WidgetNavigationPanelViewBinding.bind(view)

        adapter = NavigationPanelAdapter(
            onItemClicked = { position ->
                onItemClicked?.onItemClicked(position)
            }
        )

        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = null
    }

    private fun setValuesInternal(values: List<String>?) {
        adapter.updateItems(values ?: emptyList())
        if (values != null) {
            binding.recyclerView.scrollToPosition(values.lastIndex)
        }
    }

    companion object {

        @JvmStatic
        @BindingAdapter("onItemClick")
        fun setOnItemClickListener(
            view: NavigationPanelView,
            listener: OnItemClickListener?
        ) {
            view.onItemClicked = listener
        }
    }
}