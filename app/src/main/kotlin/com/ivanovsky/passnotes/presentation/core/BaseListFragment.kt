package com.ivanovsky.passnotes.presentation.core

import android.os.Bundle
import androidx.annotation.DrawableRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ivanovsky.passnotes.R

abstract class BaseListFragment<T> : BaseFragment() {

	private var items: T? = null //TODO: does it necessary filed???
	private lateinit var recyclerView: RecyclerView
	private lateinit var swipeRefreshLayout: SwipeRefreshLayout
	private lateinit var fab: FloatingActionButton

	protected abstract fun onCreateConfig(): Config
	protected abstract fun onCreateListAdapter(): RecyclerView.Adapter<out RecyclerView.ViewHolder>

	override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.core_base_list_fragment, container, false)

		recyclerView = view.findViewById(R.id.recycler_view)
		swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
		fab = view.findViewById(R.id.fab)

		val config = onCreateConfig()
		val adapter = onCreateListAdapter()

		val layoutManager: RecyclerView.LayoutManager

		if (config.layout == Layout.VERTICAL_LIST) {
			layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

			if (config.isDividerEnabled) {
				recyclerView.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
			}
		} else {
			throw IllegalArgumentException("Layout type is not implemented: " + config.layout)
		}

		if (!config.isFloatingActionButtonEnabled) {
			fab.hide()
		}

		if (config.floatingActionButtonIconId != -1) {
			fab.setImageResource(config.floatingActionButtonIconId)
		}

		swipeRefreshLayout.isEnabled = config.isSwipeRefreshEnabled

		recyclerView.adapter = adapter
		recyclerView.layoutManager = layoutManager

		return view
	}

	class Config(val layout: Layout = Layout.VERTICAL_LIST,
	             val isDividerEnabled: Boolean = false,
	             val isFloatingActionButtonEnabled: Boolean = false,
	             @DrawableRes val floatingActionButtonIconId: Int = -1,
	             val isSwipeRefreshEnabled: Boolean = false)

	enum class Layout {
		VERTICAL_LIST
	}
}