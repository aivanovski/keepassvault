package com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog

import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ClipboardInteractor
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.formatReadableMessage
import com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog.model.ReportErrorState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils
import com.ivanovsky.passnotes.util.StringUtils.LINE_BREAK
import kotlinx.coroutines.flow.MutableStateFlow

class ReportErrorDialogViewModel(
    private val clipboardInteractor: ClipboardInteractor,
    private val resourceProvider: ResourceProvider,
    private val args: ReportErrorDialogArgs
) : ViewModel() {

    val state = MutableStateFlow(
        createState(
            args = args,
            isCollapsed = true
        )
    )
    val openIssuesUrlEvent = SingleLiveEvent<String>()

    fun onCopyButtonClicked() {
        val message = args.error.formatReadableMessage(resourceProvider)
        val stacktrace = args.error.throwable?.stackTraceToString() ?: StringUtils.EMPTY

        if (message.isEmpty() && stacktrace.isEmpty()) {
            return
        }

        val fullText = when {
            stacktrace.isEmpty() -> message
            message.isEmpty() -> stacktrace
            else -> message + LINE_BREAK + stacktrace
        }

        clipboardInteractor.copy(
            text = fullText,
            isProtected = false
        )
    }

    fun navigateToIssues() {
        val url = resourceProvider.getString(R.string.issues_url)
        openIssuesUrlEvent.call(url)
    }

    fun onToggleStateClicked() {
        state.value = createState(
            args = args,
            isCollapsed = !state.value.isStacktraceCollapsed
        )
    }

    private fun createState(
        args: ReportErrorDialogArgs,
        isCollapsed: Boolean
    ): ReportErrorState {
        val error = args.error
        val title = error.formatReadableMessage(resourceProvider)
        val stacktrace = error.throwable?.stackTraceToString() ?: StringUtils.EMPTY

        return ReportErrorState(
            title = title,
            stacktrace = stacktrace,
            isStacktraceCollapsed = isCollapsed
        )
    }
}