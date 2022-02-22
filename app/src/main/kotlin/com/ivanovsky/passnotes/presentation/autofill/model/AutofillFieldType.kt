package com.ivanovsky.passnotes.presentation.autofill.model

enum class AutofillFieldType {
    USERNAME,
    PASSWORD,
    // TODO(autofill): implement other types
    CREDIT_CARD_HOLDER,
    CREDIT_CARD_NUMBER,
    CREDIT_CARD_EXPIRATION,
    CREDIT_CARD_EXPIRATION_YEAR,
    CREDIT_CARD_EXPIRATION_MONTH,
    CREDIT_CARD_EXPIRATION_DAY,
    CREDIT_CARD_SECURITY_CODE
}