package com.ivanovsky.passnotes.presentation.unlock

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import com.ivanovsky.passnotes.R

internal class FileSpinnerAdapter(context: Context) : BaseAdapter() {

	private val inflater = LayoutInflater.from(context)
	private val items = mutableListOf<Item>()

	fun setItem(newItems: List<Item>?) {
		items.clear()

		if (newItems != null) {
			items.addAll(newItems)
		}
	}

	override fun getCount(): Int {
		return items.size
	}

	override fun getItem(position: Int): Item {
		return items[position]
	}

	override fun getItemId(position: Int): Long {
		return position.toLong()
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val holder: ViewHolder
		val view: View

		if (convertView == null) {
			view = inflater.inflate(R.layout.unlock_spinner_item, parent, false)

			holder = ViewHolder(view)

			view.tag = holder
		} else {
			view = convertView
			holder = view.tag as ViewHolder
		}

		val item = items[position]

		holder.filenameTextView.text = item.filename
		holder.pathTextView.text = item.path
		holder.storageTypeTextView.text = item.storageType

		return view
	}

	override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
		val holder: ViewHolder
		val view: View

		if (convertView == null) {
			view = inflater.inflate(R.layout.unlock_spinner_dropdown_item, parent, false)

			holder = ViewHolder(view)

			view.tag = holder
		} else {
			view = convertView
			holder = view.tag as ViewHolder
		}

		val item = items[position]

		holder.filenameTextView.text = item.filename
		holder.pathTextView.text = item.path
		holder.storageTypeTextView.text = item.storageType

		return view
	}

	private class ViewHolder(view: View) {
		val filenameTextView = view.findViewById<TextView>(R.id.filename)!!
		val pathTextView = view.findViewById<TextView>(R.id.path)!!
		val storageTypeTextView = view.findViewById<TextView>(R.id.storage_type)!!
	}

	internal class Item(val filename: String, val path: String, val storageType: String)
}
