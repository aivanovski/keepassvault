package com.ivanovsky.passnotes.presentation.groups

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R

class GroupsAdapter(val context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	lateinit var onGroupItemClickListener: (Int) -> Unit
	lateinit var onButtonItemClickListener: () -> Unit

	private val items: MutableList<ListItem> = mutableListOf()
	private val inflater: LayoutInflater = LayoutInflater.from(context)

	fun setItems(newItems: List<ListItem>) {
		items.clear()
		items.addAll(newItems)

		notifyDataSetChanged()
	}

	override fun getItemCount(): Int {
		return items.size
	}

	override fun getItemViewType(position: Int): Int {
		return items[position].viewType
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
			RecyclerView.ViewHolder {
		val result: RecyclerView.ViewHolder

		if (viewType == VIEW_TYPE_GROUP) {
			val view = inflater.inflate(R.layout.groups_group_list_item, parent, false)
			result = GroupItemViewHolder(view)

		} else {
			val view = inflater.inflate(R.layout.groups_button_list_item, parent, false)
			result = ButtonItemViewHolder(view)
		}

		return result
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val viewType = getItemViewType(position)

		if (viewType == VIEW_TYPE_GROUP) {
			val viewHolder = holder as GroupItemViewHolder
			val item = items[position] as GroupListItem

			viewHolder.title.text = item.title
			viewHolder.count.text = item.noteCount.toString()

			viewHolder.rootLayout.setOnClickListener { onGroupItemClicked(position) }

		} else if (viewType == VIEW_TYPE_BUTTON) {
			val viewHolder = holder as ButtonItemViewHolder

			viewHolder.rootLayout.setOnClickListener { onButtonItemClicked() }
		}
	}

	private fun onGroupItemClicked(position: Int) {
		onGroupItemClickListener.invoke(position)
	}

	private fun onButtonItemClicked() {
		onButtonItemClickListener.invoke()
	}

	private inner class GroupItemViewHolder(view: View): RecyclerView.ViewHolder(view) {

		val title: TextView = view.findViewById(R.id.title)
		val count: TextView = view.findViewById(R.id.count)
		val rootLayout: ViewGroup = view.findViewById(R.id.root_layout)
	}

	private inner class ButtonItemViewHolder(view: View): RecyclerView.ViewHolder(view) {

		val rootLayout: ViewGroup = view.findViewById(R.id.root_layout)
	}

	open class ListItem(val viewType: Int)

	class GroupListItem(val title: String, val noteCount: Int): ListItem(VIEW_TYPE_GROUP)

	class ButtonListItem: ListItem(VIEW_TYPE_BUTTON)

	companion object {
		private const val VIEW_TYPE_GROUP = 1
		private const val VIEW_TYPE_BUTTON = 2
	}
}
