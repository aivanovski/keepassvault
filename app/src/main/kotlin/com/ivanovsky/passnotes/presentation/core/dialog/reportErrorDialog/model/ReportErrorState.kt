package com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog.model

data class ReportErrorState(
    val title: String,
    val stacktrace: String,
    val isStacktraceCollapsed: Boolean
)