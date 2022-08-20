package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.WidgetMaterialSpinnerBinding
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class MaterialSpinner(
    context: Context,
    attrs: AttributeSet
) : FrameLayout(context, attrs) {

    var selectedItem: String
        get() = binding.autocompleteTextView.text.toString()
        set(value) {
            binding.autocompleteTextView.setText(value, false)
        }

    var items: List<String> = emptyList()
        set(items) {
            setItemsToView(items)
            field = items
        }

    var hint: String = EMPTY
        set(value) {
            setHintToView(value)
            field = value
        }

    private val binding: WidgetMaterialSpinnerBinding
    private val textWatcher = CustomTextWatcher()
    private var onItemSelectListener: OnItemSelectListener? = null
    private var lastSelectedItem: String? = null

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.widget_material_spinner, this, true)
        binding = WidgetMaterialSpinnerBinding.bind(view)

        binding.autocompleteTextView.apply {
            setAdapter(createAdapter(items))
            addTextChangedListener(textWatcher)
        }
    }

    private fun createAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(context, android.R.layout.simple_list_item_1, items)
    }

    private fun setItemsToView(items: List<String>) {
        val oldSelectedItem = this.selectedItem
        val selectedIdx = items.indexOf(oldSelectedItem)
        val newSelectedItem = if (selectedIdx != -1) {
            items[selectedIdx]
        } else {
            items.firstOrNull() ?: EMPTY
        }

        textWatcher.isEnabled = false

        binding.autocompleteTextView.apply {
            setAdapter(createAdapter(items))
            setText(newSelectedItem, false)
        }

        textWatcher.isEnabled = true
    }

    private fun setHintToView(hint: String) {
        binding.textInputLayout.hint = hint
    }

    private inner class CustomTextWatcher : TextWatcher {

        var isEnabled: Boolean = true

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val text = s?.toString() ?: return

            if (lastSelectedItem != text) {
                lastSelectedItem = text
                val itemIndex = items.indexOf(lastSelectedItem)
                onItemSelectListener?.onItemSelected(text, itemIndex)
            }
        }

        override fun afterTextChanged(s: Editable?) {
        }
    }

    companion object {

        @JvmStatic
        @InverseBindingAdapter(attribute = "item")
        fun getSelectedItem(view: MaterialSpinner): String {
            return view.selectedItem
        }

        @JvmStatic
        @BindingAdapter("onItemSelected", "itemAttrChanged", requireAll = false)
        fun setOnSelectedItemChangeListener(
            view: MaterialSpinner,
            onItemSelected: OnItemSelectListener?,
            itemAttrChanged: InverseBindingListener?,
        ) {
            if (itemAttrChanged == null) {
                view.onItemSelectListener = onItemSelected
            } else {
                view.onItemSelectListener = OnItemSelectListener { item, itemIndex ->
                    onItemSelected?.onItemSelected(item, itemIndex)
                    itemAttrChanged.onChange()
                }
            }
        }

        @JvmStatic
        @BindingAdapter("item", "items")
        fun setSelectedItem(view: MaterialSpinner, selectedItem: String?, items: List<String>?) {
            if (items != null) {
                view.items = items
            }

            if (selectedItem != null && selectedItem != view.selectedItem) {
                view.selectedItem = selectedItem
            }
        }
    }
}