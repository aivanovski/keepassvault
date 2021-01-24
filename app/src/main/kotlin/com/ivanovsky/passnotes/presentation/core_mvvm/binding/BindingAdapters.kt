package com.ivanovsky.passnotes.presentation.core_mvvm.binding

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.ScreenState
import com.ivanovsky.passnotes.presentation.core_mvvm.ScreenStateHandler
import com.ivanovsky.passnotes.presentation.core_mvvm.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core_mvvm.adapter.ViewModelsAdapter
import com.ivanovsky.passnotes.util.getLifecycleOwner

@BindingAdapter("screenState", "screenStateHandler")
fun setScreenState(
    view: View,
    screenStateData: LiveData<ScreenState>,
    screenStateHandler: ScreenStateHandler
) {
    val screenState = screenStateData.value ?: return

    screenStateHandler.applyScreenState(view, screenState)
}

@BindingAdapter("viewModels", "viewTypes")
fun setViewModel(
    recyclerView: RecyclerView,
    viewModelsData: LiveData<List<BaseCellViewModel>>,
    viewTypes: ViewModelTypes
) {
    val lifecycleOwner = recyclerView.context.getLifecycleOwner()
        ?: throw IllegalStateException("context doesn't have LifecycleOwner")

    val adapter = (recyclerView.adapter as? ViewModelsAdapter)
        ?: ViewModelsAdapter(
            lifecycleOwner,
            viewTypes
        ).also {
            recyclerView.adapter = it
        }

    viewModelsData.value?.let { viewModels ->
        adapter.updateItems(viewModels)
    }
}

@BindingAdapter("errorText")
fun setError(textInputLayout: TextInputLayout, errorData: LiveData<String?>) {
    textInputLayout.error = errorData.value
}

@BindingAdapter("visible")
fun setVisible(view: View, isVisible: Boolean) {
    view.isVisible = isVisible
}

@BindingAdapter("textWatcher")
fun addTextWatcher(editText: TextInputEditText, onTextChangeListener: (text: String) -> Unit) {
    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            onTextChangeListener.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    })
}
