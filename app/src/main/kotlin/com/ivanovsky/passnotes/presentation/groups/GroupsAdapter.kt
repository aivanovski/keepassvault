package com.ivanovsky.passnotes.presentation.groups

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R

class GroupsAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var onListItemClickListener: (Int) -> Unit
    lateinit var onListItemLongClickListener: (Int) -> Unit

    private val items: MutableList<ListItem> = mutableListOf()
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    fun setItems(newItems: List<ListItem>) {
        items.clear()
        items.addAll(newItems)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GROUP -> {
                val view = inflater.inflate(R.layout.groups_group_list_item, parent, false)
                GroupItemViewHolder(view)

            }
            VIEW_TYPE_NOTE -> {
                val view = inflater.inflate(R.layout.groups_note_list_item, parent, false)
                NoteItemViewHolder(view)
            }
            VIEW_TYPE_BUTTON -> {
                val view = inflater.inflate(R.layout.groups_button_list_item, parent, false)
                ButtonItemViewHolder(view)
            }
            else -> throw IllegalStateException("Incorrect viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)

        if (viewType == VIEW_TYPE_GROUP) {
            val viewHolder = holder as GroupItemViewHolder
            val item = items[position] as GroupListItem

            viewHolder.title.text = item.title
            viewHolder.count.text = formatCountsForGroup(item)

            viewHolder.rootLayout.setOnClickListener { onListItemClicked(position) }
            viewHolder.rootLayout.setOnLongClickListener { onListItemLongClicked(position) }

        } else if (viewType == VIEW_TYPE_BUTTON) {
            val viewHolder = holder as ButtonItemViewHolder

            viewHolder.rootLayout.setOnClickListener { onListItemClicked(position) }
            viewHolder.rootLayout.setOnLongClickListener { onListItemLongClicked(position) }

        } else if (viewType == VIEW_TYPE_NOTE) {
            val viewHolder = holder as NoteItemViewHolder
            val item = items[position] as NoteListItem

            viewHolder.title.text = item.title

            viewHolder.rootLayout.setOnClickListener { onListItemClicked(position) }
            viewHolder.rootLayout.setOnLongClickListener { onListItemLongClicked(position) }
        }
    }

    private fun formatCountsForGroup(item: GroupListItem): String {
        val notes = item.noteCount
        val groups = item.childGroupCount

        return if (notes == 0 && groups == 0) {
            ""
        } else if (notes > 0 && groups == 0) {
            context.getString(R.string.notes_with_count, notes)
        } else if (notes == 0 && groups > 0) {
            context.getString(R.string.groups_with_count, groups)
        } else {
            context.getString(R.string.groups_and_notes_with_count, notes, groups)
        }
    }

    private fun onListItemClicked(position: Int) {
        onListItemClickListener.invoke(position)
    }

    private fun onListItemLongClicked(position: Int): Boolean {
        onListItemLongClickListener.invoke(position)
        return true
    }

    private class GroupItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val count: TextView = view.findViewById(R.id.count)
        val rootLayout: ViewGroup = view.findViewById(R.id.root_layout)
    }

    private class ButtonItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rootLayout: ViewGroup = view.findViewById(R.id.root_layout)
    }

    private class NoteItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val rootLayout: ViewGroup = view.findViewById(R.id.root_layout)
    }

    abstract class ListItem(val viewType: Int)

    data class GroupListItem(
        val title: String,
        val noteCount: Int,
        val childGroupCount: Int
    ) : ListItem(VIEW_TYPE_GROUP)

    data class NoteListItem(val title: String) : ListItem(VIEW_TYPE_NOTE)

    class ButtonListItem : ListItem(VIEW_TYPE_BUTTON)

    companion object {
        private const val VIEW_TYPE_GROUP = 1
        private const val VIEW_TYPE_BUTTON = 2
        private const val VIEW_TYPE_NOTE = 3
    }
}
