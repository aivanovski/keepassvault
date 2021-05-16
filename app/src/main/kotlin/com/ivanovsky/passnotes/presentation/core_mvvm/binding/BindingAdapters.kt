package com.ivanovsky.passnotes.presentation.core_mvvm.binding

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.widget.SecureTextView
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.ScreenState
import com.ivanovsky.passnotes.presentation.core_mvvm.ScreenStateHandler
import com.ivanovsky.passnotes.presentation.core_mvvm.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core_mvvm.adapter.ViewModelsAdapter
import com.ivanovsky.passnotes.presentation.note_editor.view.TextTransformationMethod
import com.ivanovsky.passnotes.presentation.note_editor.view.TextTransformationMethod.PASSWORD
import com.ivanovsky.passnotes.presentation.note_editor.view.TextTransformationMethod.PLANE_TEXT
import com.ivanovsky.passnotes.presentation.note_editor.view.SecretInputType
import com.ivanovsky.passnotes.presentation.note_editor.view.SecretInputType.DIGITS
import com.ivanovsky.passnotes.presentation.note_editor.view.SecretInputType.TEXT
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputLines
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputType
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
fun setError(textInputLayout: TextInputLayout, errorData: LiveData<String?>?) {
    textInputLayout.error = errorData?.value
}

@BindingAdapter("bind:visible")
fun setVisible(view: View, isVisible: Boolean) {
    view.isVisible = isVisible
}

@BindingAdapter("onTextChanged")
fun setOnTextChangedListener(editText: TextInputEditText, onTextChangeListener: OnTextChangeListener?) {
    val existingListener = editText.getTag(R.id.tagTextWatcher) as? TextWatcher
    existingListener?.let {
        editText.removeTextChangedListener(it)
    }

    if (onTextChangeListener == null) {
        return
    }

    val listener = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            onTextChangeListener.onTextChanged(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }
    editText.setTag(R.id.tagTextWatcher, listener)
    editText.addTextChangedListener(listener)
}

@BindingAdapter("bind:src")
fun setImageResource(imageView: ImageView, @DrawableRes imageResourceId: Int) {
    imageView.setImageResource(imageResourceId)
}

@BindingAdapter("bind:isTextHidden")
fun setTextHidden(textView: SecureTextView, isHidden: LiveData<Boolean>?) {
    val isTextHidden = isHidden?.value ?: false
    if (isTextHidden) {
        textView.hideText()
    } else {
        textView.showText()
    }
}

@BindingAdapter("onLongClick")
fun setOnLongClickListener(
    view: View,
    onLongClicked: () -> Unit
) {
    view.setOnLongClickListener {
        onLongClicked.invoke()
        true
    }
}

@BindingAdapter("textInputLines")
fun setInputLines(editText: EditText, inputLines: TextInputLines?) {
    when (inputLines) {
        TextInputLines.SINGLE_LINE -> {
            editText.minLines = 1
            editText.maxLines = 1
        }
        TextInputLines.MULTIPLE_LINES -> {
            editText.minLines = 1
            editText.maxLines = 5
        }
    }
}

@BindingAdapter("textInputType")
fun setInputType(editText: EditText, textInputType: TextInputType?) {
    if (textInputType == null) {
        return
    }

    val inputType = when (textInputType) {
        TextInputType.URL -> InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_URI
        TextInputType.TEXT -> InputType.TYPE_CLASS_TEXT
        TextInputType.EMAIL -> InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        TextInputType.TEXT_CAP_SENTENCES -> {
            InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
    }
    editText.setRawInputType(inputType)
}

//fun setTransformationMethod(
//    textView: TextView,
//    transformationMethod: LiveData<TextTransformationMethod>?
//) {
//    setTransformationMethod(textView, transformationMethod?.value)
//}

@BindingAdapter("textTransformationMethod")
fun setTransformationMethod(
    textView: TextView,
    transformationMethod: TextTransformationMethod?
) {
    transformationMethod?.let {
        textView.transformationMethod = when (it) {
            PASSWORD -> PasswordTransformationMethod.getInstance()
            PLANE_TEXT -> HideReturnsTransformationMethod.getInstance()
        }
    }
}

@BindingAdapter("secretInputType")
fun setSecretInputType(
    editText: EditText,
    inputType: SecretInputType?
) {
    if (inputType == null) {
        return
    }
    editText.setRawInputType(
        when (inputType) {
            TEXT -> InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_PASSWORD
            DIGITS -> InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
    )
}