package com.ivanovsky.passnotes.presentation.core.binding

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.adapter.StringArraySpinnerAdapter
import com.ivanovsky.passnotes.presentation.core.widget.CellLinearLayout
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView
import com.ivanovsky.passnotes.presentation.core.widget.SecureTextView
import com.ivanovsky.passnotes.presentation.core.widget.TextMovementMethod
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnButtonClickListener
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnItemSelectListener
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnSliderValueSelectedListener
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape
import com.ivanovsky.passnotes.presentation.core.widget.entity.SecretInputType
import com.ivanovsky.passnotes.presentation.core.widget.entity.SecretInputType.DIGITS
import com.ivanovsky.passnotes.presentation.core.widget.entity.SecretInputType.TEXT
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextInputLines
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextInputType
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextTransformationMethod
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextTransformationMethod.PASSWORD
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextTransformationMethod.PLANE_TEXT

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
fun setViewModels(
    view: CellLinearLayout,
    viewModels: List<BaseCellViewModel>?,
    viewTypes: ViewModelTypes
) {
    view.setViewTypes(viewTypes)
    view.setViewModels(viewModels ?: emptyList())
}

@BindingAdapter("errorText")
fun setError(textInputLayout: TextInputLayout, errorData: LiveData<String?>?) {
    textInputLayout.error = errorData?.value
}

@BindingAdapter("visible")
fun setVisible(view: View, isVisible: Boolean) {
    view.isVisible = isVisible
}

@BindingAdapter("onTextChanged")
fun setOnTextChangedListener(
    editText: TextInputEditText,
    onTextChangeListener: OnTextChangeListener?
) {
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

@BindingAdapter("imageResourceId")
fun setImageResource(imageView: ImageView, @DrawableRes imageResourceId: Int?) {
    if (imageResourceId != null) {
        imageView.setImageResource(imageResourceId)
    } else {
        imageView.setImageDrawable(null)
    }
}

@BindingAdapter("isTextHidden")
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
    if (inputLines == null) {
        return
    }

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
        TextInputType.DIGITS -> InputType.TYPE_CLASS_NUMBER
        TextInputType.URL -> InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_URI
        TextInputType.TEXT -> InputType.TYPE_CLASS_TEXT
        TextInputType.EMAIL -> {
            InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        }

        TextInputType.TEXT_CAP_SENTENCES -> {
            InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
    }
    editText.setRawInputType(inputType)
}

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

@BindingAdapter("textMovementMethod")
fun setMovementMethod(
    textView: TextView,
    movementMethod: TextMovementMethod?
) {
    if (movementMethod == null) {
        return
    }

    when (movementMethod) {
        TextMovementMethod.LINK_MOVEMENT_METHOD -> {
            textView.movementMethod = LinkMovementMethod.getInstance()
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

@BindingAdapter("onButtonClicked")
fun setOnButtonClickListener(
    errorPanelView: ErrorPanelView,
    listener: OnButtonClickListener?
) {
    errorPanelView.buttonClickListener = listener
}

@BindingAdapter("onItemSelected")
fun setSpinnerItemSelectedListener(
    spinner: Spinner,
    onItemSelectedListener: OnItemSelectListener?
) {
    if (onItemSelectedListener != null) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val adapter = parent?.adapter as? StringArraySpinnerAdapter ?: return
                onItemSelectedListener.onItemSelected(adapter.getItem(position), position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    } else {
        spinner.onItemSelectedListener = null
    }
}

@BindingAdapter("onSliderValueChanged")
fun setSliderValueSelectedListener(
    slider: Slider,
    listener: OnSliderValueSelectedListener?
) {
    slider.clearOnSliderTouchListeners()
    if (listener == null) {
        return
    }

    slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {
        }

        override fun onStopTrackingTouch(slider: Slider) {
            listener.onValueSelected(slider.value.toInt())
        }
    })
}

@BindingAdapter("items")
fun setSpinnerItems(
    spinner: Spinner,
    items: List<String>?
) {
    val nonEmptyItems = items ?: emptyList()

    spinner.adapter = if (spinner.adapter == null) {
        StringArraySpinnerAdapter(spinner.context, nonEmptyItems)
    } else {
        (spinner.adapter as? StringArraySpinnerAdapter)
            ?.apply {
                updateItems(nonEmptyItems)
            }
    }
}

@BindingAdapter("backgroundShapeColor", "backgroundShape")
fun setMaterialBackground(
    view: View,
    shapeColor: Int?,
    shape: RoundedShape?
) {
    val color = shapeColor ?: Color.RED
    val shapeModel = buildShapeAppearanceModel(view.context, shape ?: RoundedShape.ALL)
    val drawable = MaterialShapeDrawable(shapeModel)
    drawable.fillColor = ColorStateList.valueOf(color)

    view.background = drawable
}

private fun buildShapeAppearanceModel(context: Context, shape: RoundedShape): ShapeAppearanceModel {
    val builder = ShapeAppearanceModel.Builder()

    when (shape) {
        RoundedShape.ALL -> {
            builder.setAllCorners(
                CornerFamily.ROUNDED,
                context.resources.getDimension(R.dimen.card_corner_radius)
            )
        }

        RoundedShape.BOTTOM -> {
            builder.setBottomLeftCorner(
                CornerFamily.ROUNDED,
                context.resources.getDimension(R.dimen.card_corner_radius)
            )
            builder.setBottomRightCorner(
                CornerFamily.ROUNDED,
                context.resources.getDimension(R.dimen.card_corner_radius)
            )
        }

        RoundedShape.TOP -> {
            builder.setTopLeftCorner(
                CornerFamily.ROUNDED,
                context.resources.getDimension(R.dimen.card_corner_radius)
            )
            builder.setTopRightCorner(
                CornerFamily.ROUNDED,
                context.resources.getDimension(R.dimen.card_corner_radius)
            )
        }

        RoundedShape.NONE -> {
        }
    }

    return builder.build()
}

@BindingAdapter("materialBackgroundColor")
fun setMaterialBackgroundColor(
    imageButton: ImageButton,
    color: Int?
) {
    if (color == null) {
        return
    }

    val cornerSize = imageButton.context.resources.getDimension(R.dimen.quarter_margin)

    val drawable = MaterialShapeDrawable()
        .apply {
            shapeAppearanceModel = ShapeAppearanceModel.Builder()
                .setAllCornerSizes(cornerSize)
                .build()
        }
    DrawableCompat.setTint(drawable, color)

    imageButton.background = drawable
}

@BindingAdapter("isEnabled")
fun setEnabled(
    view: View,
    isEnabled: Boolean?
) {
    view.isEnabled = isEnabled ?: false
}

@BindingAdapter("isBold")
fun setBoldStyle(
    textView: TextView,
    isBold: Boolean?
) {
    if (isBold == true) {
        textView.setTypeface(null, Typeface.BOLD)
    } else {
        textView.setTypeface(null, Typeface.NORMAL)
    }
}

@BindingAdapter("isStrikeThrough")
fun setStrikeThrough(
    textView: TextView,
    isStrikeThrough: Boolean?
) {
    if (isStrikeThrough == true) {
        textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    } else if (textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG != 0) {
        textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }
}