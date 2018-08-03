package com.ivanovsky.passnotes.presentation.note

import android.content.Context
import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.NoteListItemSimplePropertyBinding

class NoteAdapter(context: Context):
		RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

	lateinit var onCopyButtonClickListener: (Int) -> Unit

	private val items = mutableListOf<Item>()
	private val inflater = LayoutInflater.from(context)

	fun setItems(newItems: List<Item>) {
		items.clear()
		items.addAll(newItems)
	}

	override fun getItemCount(): Int {
		return items.size
	}

	override fun getItemViewType(position: Int): Int {
		return items[position].viewType
	}

	override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): NoteAdapter.ViewHolder {
		val result: NoteAdapter.ViewHolder

		val binding = DataBindingUtil.inflate<NoteListItemSimplePropertyBinding>(inflater,
				R.layout.note_list_item_simple_property, parent, false)

		result = NotePropertyViewHolder(binding)

		return result
	}

	override fun onBindViewHolder(holder: NoteAdapter.ViewHolder?, position: Int) {
		val viewType = getItemViewType(position)

		if (viewType == VIEW_TYPE_NOTE_PROPERTY) {
			val viewHolder = holder as NotePropertyViewHolder
			val item = items[position] as NotePropertyItem
			val binding = viewHolder.binding

			binding.propertyName.text = item.propertyName
			binding.propertyValue.text = item.propertyValue

			if (item.isPropertyValueHidden) {
				binding.propertyValue.hideText()
			} else {
				binding.propertyValue.showText()
			}

			binding.visibilityButton.setOnClickListener { onVisibilityButtonClicked(position) }
			binding.copyButton.setOnClickListener { onCopyButtonClicked(position) }

			if (item.isVisibilityBtnVisible) {
				binding.visibilityButton.visibility = View.VISIBLE
			} else {
				binding.visibilityButton.visibility = View.GONE
			}

			if (item.isCopyBtnVisible) {
				binding.copyButton.visibility = View.VISIBLE
			} else {
				binding.copyButton.visibility = View.GONE
			}

			if ((item.isVisibilityBtnVisible && !item.isCopyBtnVisible)
					|| (!item.isVisibilityBtnVisible && item.isCopyBtnVisible)) {
				binding.buttonDivider.visibility = View.GONE
			} else if (!item.isVisibilityBtnVisible && !item.isCopyBtnVisible) {
				binding.buttonDivider.visibility = View.GONE
			} else {
				binding.buttonDivider.visibility = View.VISIBLE
			}
		}
	}

	private fun onVisibilityButtonClicked(position: Int) {
		val item = items[position] as NotePropertyItem

		item.isPropertyValueHidden = !item.isPropertyValueHidden

		notifyItemChanged(position)
	}

	private fun onCopyButtonClicked(position: Int) {
		onCopyButtonClickListener.invoke(position)
	}

	abstract class ViewHolder(private val root: View): RecyclerView.ViewHolder(root)

	private class NotePropertyViewHolder(val binding: NoteListItemSimplePropertyBinding):
			ViewHolder(binding.root)

	abstract class Item(val viewType: Int)

	class NotePropertyItem(val propertyName: String,
	                       val propertyValue: String,
	                       val isVisibilityBtnVisible: Boolean,
	                       val isCopyBtnVisible: Boolean,
	                       var isPropertyValueHidden: Boolean) : Item(VIEW_TYPE_NOTE_PROPERTY)

	companion object {
		private const val VIEW_TYPE_NOTE_PROPERTY = 1
	}
}