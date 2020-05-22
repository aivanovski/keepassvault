package com.ivanovsky.passnotes.presentation.note

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.widget.SecureTextView

class NoteAdapter(context: Context) :
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteAdapter.ViewHolder {
        val result: NoteAdapter.ViewHolder

        val view = inflater.inflate(R.layout.note_list_item_simple_property, parent, false)

        result = NotePropertyViewHolder(view)

        return result
    }

    override fun onBindViewHolder(holder: NoteAdapter.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)

        if (viewType == VIEW_TYPE_NOTE_PROPERTY) {
            val viewHolder = holder as NotePropertyViewHolder
            val item = items[position] as NotePropertyItem

            viewHolder.propertyName.text = item.propertyName
            viewHolder.propertyValue.text = item.propertyValue

            if (item.isPropertyValueHidden) {
                viewHolder.propertyValue.hideText()
            } else {
                viewHolder.propertyValue.showText()
            }

            viewHolder.visibilityButton.setOnClickListener { onVisibilityButtonClicked(position) }
            viewHolder.copyButton.setOnClickListener { onCopyButtonClicked(position) }

            if (item.isVisibilityBtnVisible) {
                viewHolder.visibilityButton.visibility = View.VISIBLE
            } else {
                viewHolder.visibilityButton.visibility = View.GONE
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

    abstract class ViewHolder(root: View) : RecyclerView.ViewHolder(root)

    private class NotePropertyViewHolder(view: View) : ViewHolder(view) {

        val propertyName: TextView = view.findViewById(R.id.property_name)
        val propertyValue: SecureTextView = view.findViewById(R.id.property_value)
        val visibilityButton: View = view.findViewById(R.id.visibility_button)
        val copyButton: View = view.findViewById(R.id.copy_button)
    }

    abstract class Item(val viewType: Int)

    class NotePropertyItem(
        val propertyName: String,
        val propertyValue: String,
        val isVisibilityBtnVisible: Boolean,
        var isPropertyValueHidden: Boolean
    ) : Item(VIEW_TYPE_NOTE_PROPERTY)

    companion object {
        private const val VIEW_TYPE_NOTE_PROPERTY = 1
    }
}