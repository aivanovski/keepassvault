package com.ivanovsky.passnotes.domain.test.entity

import com.ivanovsky.passnotes.data.entity.TestAutofillData

data class TestArguments(
    val isResetAppData: Boolean,
    val isResetTestData: Boolean,
    val testFakeFileName: String?,
    val testAutofillData: TestAutofillData?
)