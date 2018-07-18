package com.ivanovsky.passnotes.ui.core.adapter

import android.content.Context
import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.ListItemSingleLineBinding

class SingleLineAdapter(context: Context):
		RecyclerView.Adapter<SingleLineAdapter.ViewHolder>() {

	private val items = mutableListOf<Item>()
	private val inflater = LayoutInflater.from(context)

	fun setItems(newItems: List<Item>) {
		items.clear()
		items.addAll(newItems)
	}

	override fun getItemCount(): Int {
		return items.size
	}

	override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
		val binding = DataBindingUtil.inflate<ListItemSingleLineBinding>(inflater,
				R.layout.list_item_single_line, parent, false)
		return ViewHolder(binding)
	}

	override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
		holder!!.binding.primaryTextView.text = items[position].title
	}

	inner class ViewHolder(val binding: ListItemSingleLineBinding): RecyclerView.ViewHolder(binding.root)

	class Item(val title: String)
}