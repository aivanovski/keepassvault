package com.ivanovsky.passnotes.presentation.filepicker

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ivanovsky.passnotes.R

class FilePickerAdapter(context: Context) :
		RecyclerView.Adapter<FilePickerAdapter.ViewHolder>() {

	lateinit var onItemClickListener: (Int) -> Unit

	private val items = mutableListOf<Item>()
	private val inflater = LayoutInflater.from(context)

	fun setItems(newItems: List<Item>) {
		items.clear()
		items.addAll(newItems)
	}

	override fun getItemCount(): Int {
		return items.size
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = inflater.inflate(R.layout.fillepicker_list_item, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = items[position]

		holder.icon.setImageResource(item.iconResId)
		holder.primaryText.text = item.title
		holder.secondaryText.text = item.description
		holder.selectedBackground.visibility = if (item.selected) View.VISIBLE else View.GONE

		holder.root.setOnClickListener { onItemClicked(position) }
	}

	private fun onItemClicked(position: Int) {
		onItemClickListener.invoke(position)
	}

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

		val root = view.findViewById<ViewGroup>(R.id.root_layout)
		val icon = view.findViewById<ImageView>(R.id.icon)
		val primaryText = view.findViewById<TextView>(R.id.primary_text)
		val secondaryText = view.findViewById<TextView>(R.id.secondary_text)
		val selectedBackground = view.findViewById<View>(R.id.selected_background)
	}

	class Item(@DrawableRes val iconResId: Int,
	           val title: String,
	           val description: String,
	           var selected: Boolean)
}