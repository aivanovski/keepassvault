package com.ivanovsky.passnotes.presentation.note.cells.model

import com.ivanovsky.passnotes.domain.otp.model.OtpToken
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape

data class OtpPropertyCellModel(
    override val id: String,
    val title: String,
    val token: OtpToken,
    val backgroundShape: RoundedShape,
    val backgroundColor: Int,
) : BaseCellModel()