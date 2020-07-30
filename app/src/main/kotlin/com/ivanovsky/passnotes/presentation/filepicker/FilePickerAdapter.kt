package com.ivanovsky.passnotes.presentation.filepicker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
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
        val root: ViewGroup = view.findViewById(R.id.root_layout)
        val icon: ImageView = view.findViewById(R.id.icon)
        val primaryText: TextView = view.findViewById(R.id.primary_text)
        val secondaryText: TextView = view.findViewById(R.id.secondary_text)
        val selectedBackground: View = view.findViewById(R.id.selected_background)
    }

    data class Item(
        @DrawableRes val iconResId: Int,
        val title: String,
        val description: String,
        var selected: Boolean
    )
}