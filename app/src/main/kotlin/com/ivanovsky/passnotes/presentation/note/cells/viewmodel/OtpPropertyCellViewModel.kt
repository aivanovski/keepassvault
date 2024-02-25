
package com.ivanovsky.passnotes.presentation.note.cells.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.ivanovsky.passnotes.domain.otp.HotpGenerator
import com.ivanovsky.passnotes.domain.otp.OtpCodeFormatter
import com.ivanovsky.passnotes.domain.otp.OtpFlowFactory
import com.ivanovsky.passnotes.domain.otp.OtpGenerator
import com.ivanovsky.passnotes.domain.otp.TotpGenerator
import com.ivanovsky.passnotes.domain.otp.model.OtpToken
import com.ivanovsky.passnotes.domain.otp.model.OtpTokenType
import com.ivanovsky.passnotes.presentation.core.BaseMutableCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.note.cells.model.OtpPropertyCellModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class OtpPropertyCellViewModel(
    initModel: OtpPropertyCellModel,
    private val eventProvider: EventProvider
) : BaseMutableCellViewModel<OtpPropertyCellModel>(initModel) {

    private val generator = MutableStateFlow(createGenerator(initModel.token))

    val title = MutableLiveData(initModel.title)
    val isProgressVisible = MutableLiveData(determineProgressVisibility(initModel.token))

    val code = generator
        .flatMapLatest { generator -> buildCodeFlow(generator) }
        .map { code -> OtpCodeFormatter.format(code) }
        .asLiveData()

    val progress = generator
        .flatMapLatest { generator -> buildProgressFlow(generator) }
        .asLiveData()

    override fun setModel(newModel: OtpPropertyCellModel) {
        super.setModel(newModel)
        generator.value = createGenerator(newModel.token)
        title.value = newModel.title
        isProgressVisible.value = determineProgressVisibility(newModel.token)
    }

    fun onClicked() {
        val code = generator.value.generateCode()
        eventProvider.send((CLICK_EVENT to code).toEvent())
    }

    private fun determineProgressVisibility(token: OtpToken): Boolean {
        return token.type == OtpTokenType.TOTP
    }

    private fun createGenerator(token: OtpToken): OtpGenerator {
        return when (token.type) {
            OtpTokenType.TOTP -> TotpGenerator(model.token)
            OtpTokenType.HOTP -> HotpGenerator(model.token)
        }
    }

    private fun buildCodeFlow(generator: OtpGenerator): Flow<String> {
        return when (generator.token.type) {
            OtpTokenType.TOTP -> OtpFlowFactory.createCodeFlow(generator as TotpGenerator)
            OtpTokenType.HOTP -> flowOf(generator.generateCode())
        }
    }

    private fun buildProgressFlow(generator: OtpGenerator): Flow<Int> {
        return when (generator.token.type) {
            OtpTokenType.TOTP -> OtpFlowFactory.createProgressFlow(generator as TotpGenerator)
            OtpTokenType.HOTP -> flowOf(0)
        }
    }

    companion object {
        val CLICK_EVENT = OtpPropertyCellViewModel::class.qualifiedName + "_clickEvent"
    }
}