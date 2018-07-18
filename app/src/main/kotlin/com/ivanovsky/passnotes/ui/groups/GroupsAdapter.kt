package com.ivanovsky.passnotes.ui.groups

import android.content.Context
import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.GroupsButtonListItemBinding
import com.ivanovsky.passnotes.databinding.GroupsGroupListItemBinding

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

	override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int):
			RecyclerView.ViewHolder {
		val result: RecyclerView.ViewHolder

		if (viewType == VIEW_TYPE_GROUP) {
			val binding = DataBindingUtil.inflate<GroupsGroupListItemBinding>(inflater,
					R.layout.groups_group_list_item, parent, false)
			result = GroupItemViewHolder(binding)

		} else {
			val binding = DataBindingUtil.inflate<GroupsButtonListItemBinding>(inflater,
					R.layout.groups_button_list_item, parent, false)
			result = ButtonItemViewHolder(binding)
		}

		return result
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
		val viewType = getItemViewType(position)

		if (viewType == VIEW_TYPE_GROUP) {
			val viewHolder = holder as GroupItemViewHolder
			val item = items[position] as GroupListItem

			viewHolder.binding.title.text = item.title
			viewHolder.binding.count.text = item.noteCount.toString()

			viewHolder.binding.rootLayout.setOnClickListener { onGroupItemClicked(position) }

		} else if (viewType == VIEW_TYPE_BUTTON) {
			val viewHolder = holder as ButtonItemViewHolder

			viewHolder.binding.rootLayout.setOnClickListener { onButtonItemClicked() }
		}
	}

	private fun onGroupItemClicked(position: Int) {
		onGroupItemClickListener.invoke(position)
	}

	private fun onButtonItemClicked() {
		onButtonItemClickListener.invoke()
	}

	private inner class GroupItemViewHolder(val binding: GroupsGroupListItemBinding):
			RecyclerView.ViewHolder(binding.root)

	private inner class ButtonItemViewHolder(val binding: GroupsButtonListItemBinding):
			RecyclerView.ViewHolder(binding.root)

	open class ListItem(val viewType: Int)

	class GroupListItem(val title: String, val noteCount: Int): ListItem(VIEW_TYPE_GROUP)

	class ButtonListItem: ListItem(VIEW_TYPE_BUTTON)

	companion object {
		private const val VIEW_TYPE_GROUP = 1
		private const val VIEW_TYPE_BUTTON = 2
	}
}
