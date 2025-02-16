package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.databinding.ViewErrorPanelBinding
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.formatReadableMessage
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog.ReportErrorDialog
import com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog.ReportErrorDialogArgs
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnButtonClickListener
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class ErrorPanelView : FrameLayout {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context, null)

    var state: ScreenState? = null
        set(value) {
            field = value
            applyState(value)
        }

    var actionButtonClickListener: OnButtonClickListener? = null

    private val resourceProvider = ResourceProvider(context)
    private val binding: ViewErrorPanelBinding
    private var currentError: OperationError? = null

    init {
        binding = ViewErrorPanelBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

        binding.actionButton.setOnClickListener { actionButtonClickListener?.onButtonClicked() }
        binding.reportButton.setOnClickListener { onReportButtonClicked() }
        binding.closeIcon.setOnClickListener { onCloseIconClicked() }
    }

    private fun applyState(screenState: ScreenState?) {
        if (screenState == null) return

        currentError = screenState.error

        binding.text.text = screenState.error?.formatReadableMessage(resourceProvider) ?: EMPTY
        binding.actionButton.text = screenState.errorButtonText
        binding.actionButton.isVisible = (!screenState.errorButtonText.isNullOrEmpty())

        binding.root.isVisible = screenState.isDisplayingErrorPanel
    }

    private fun onCloseIconClicked() {
        binding.root.isVisible = false
    }

    private fun onReportButtonClicked() {
        val error = currentError ?: return

        val fragmentManager = (context as AppCompatActivity).supportFragmentManager

        val dialog = ReportErrorDialog.newInstance(
            args = ReportErrorDialogArgs(error)
        )
        dialog.show(fragmentManager, ReportErrorDialog.TAG)
    }
}