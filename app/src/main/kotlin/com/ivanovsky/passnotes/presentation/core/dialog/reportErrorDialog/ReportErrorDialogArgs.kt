package com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog

import com.ivanovsky.passnotes.data.entity.OperationError
import java.io.Serializable

data class ReportErrorDialogArgs(
    val error: OperationError
) : Serializable