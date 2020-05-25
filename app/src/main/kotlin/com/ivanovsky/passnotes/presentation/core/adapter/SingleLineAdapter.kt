package com.ivanovsky.passnotes.presentation.core.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R

class SingleLineAdapter(context: Context) :
    RecyclerView.Adapter<SingleLineAdapter.ViewHolder>() {

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
        val view = inflater.inflate(R.layout.list_item_single_line, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.primaryText.text = items[position].title
        holder.primaryText.setOnClickListener { onItemClicked(position) }
    }

    private fun onItemClicked(position: Int) {
        onItemClickListener.invoke(position)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val primaryText: TextView = view.findViewById(R.id.primary_text)
    }

    class Item(val title: String)
}