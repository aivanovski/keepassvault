package com.ivanovsky.passnotes.presentation.core.adapter

import android.content.Context
import android.widget.ArrayAdapter

class StringArraySpinnerAdapter(
    context: Context,
    items: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    fun updateItems(newItems: List<String>) {
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): String {
        return super.getItem(position) as String
    }
}