package com.ivanovsky.passnotes.domain.otp

import com.ivanovsky.passnotes.domain.otp.model.OtpToken

fun OtpToken.createCodePlaceholder(): String {
    return "-".repeat(this.digits)
}