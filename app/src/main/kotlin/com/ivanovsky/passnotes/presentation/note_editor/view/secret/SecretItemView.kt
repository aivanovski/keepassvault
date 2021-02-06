package com.ivanovsky.passnotes.presentation.note_editor.view.secret

import android.content.Context
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputLayout
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseItemView
import com.ivanovsky.passnotes.presentation.note_editor.view.secret.SecretInputType.DIGITS
import com.ivanovsky.passnotes.presentation.note_editor.view.secret.SecretInputType.TEXT
import com.ivanovsky.passnotes.util.setVisible
import java.util.*

class SecretItemView(
    context: Context
) : ConstraintLayout(context), BaseItemView<SecretDataItem> {

    private val passwordEditText: EditText
    private val confirmationEditText: EditText
    private val passwordLayout: TextInputLayout
    private val confirmationLayout: TextInputLayout
    private val visibilityButton: View
    private var isConfirmationVisible = true
    private lateinit var item: SecretDataItem

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.note_editor_secret_item_view, this)

        passwordEditText = view.findViewById(R.id.password)
        confirmationEditText = view.findViewById(R.id.confirmation)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        confirmationLayout = view.findViewById(R.id.confirmation_layout)
        visibilityButton = view.findViewById(R.id.visibility_button)

        visibilityButton.setOnClickListener {
            onVisibilityButtonClicked()
        }

        confirmationEditText.transformationMethod = PasswordTransformationMethod.getInstance()
    }

    private fun onVisibilityButtonClicked() {
        isConfirmationVisible = isConfirmationVisible.not()

        confirmationEditText.setVisible(isConfirmationVisible)

        passwordEditText.transformationMethod = if (isConfirmationVisible) {
            PasswordTransformationMethod.getInstance()
        } else {
            HideReturnsTransformationMethod.getInstance()
        }
    }

    override fun getDataItem(): SecretDataItem {
        return item.copy(
            value = getPassword()
        )
    }

    override fun setDataItem(item: SecretDataItem) {
        val context = this.context ?: return

        this.item = item

        passwordLayout.hint = item.name
        confirmationLayout.hint =
            context.getString(R.string.confirm) + " " + item.name.toLowerCase(Locale.getDefault())

        passwordEditText.setText(item.value)
        confirmationEditText.setText(item.value)

        applySecretInputType(item.secretInputType)
    }

    private fun applySecretInputType(secretInputType: SecretInputType) {
        val inputType = when (secretInputType) {
            TEXT -> InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_PASSWORD
            DIGITS -> InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        passwordEditText.setRawInputType(inputType)
        confirmationEditText.setRawInputType(inputType)

        passwordEditText.transformationMethod = if (isConfirmationVisible) {
            PasswordTransformationMethod.getInstance()
        } else {
            HideReturnsTransformationMethod.getInstance()
        }
    }

    override fun isDataValid(): Boolean {
        return !isConfirmationVisible || getPassword() == getConfirmation()
    }

    override fun displayError() {
        if (!isConfirmationVisible) {
            return
        }

        val context = this.context

        if (getPassword() != getConfirmation()) {
            confirmationEditText.error = context.getString(R.string.does_not_match_with_password)
        }
    }

    private fun getPassword(): String {
        return passwordEditText.text.toString().trim()
    }

    private fun getConfirmation(): String {
        return confirmationEditText.text.toString().trim()
    }
}