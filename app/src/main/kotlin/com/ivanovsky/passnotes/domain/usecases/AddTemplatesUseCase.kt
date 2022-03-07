package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.keepass.TemplateFactory.createDefaultTemplates
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext

class AddTemplatesUseCase(
    private val dbRepo: EncryptedDatabaseRepository,
    private val dispatchers: DispatcherProvider,
    private val observerBus: ObserverBus
) {

    suspend fun addTemplates(): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val addTemplatesResult = dbRepo.templateRepository.addTemplates(createDefaultTemplates())

            if (addTemplatesResult.isSucceededOrDeferred) {
                observerBus.notifyGroupDataSetChanged()

                val getTemplateGroupUidResult = dbRepo.templateRepository.getTemplateGroupUid()
                if (getTemplateGroupUidResult.isSucceededOrDeferred) {
                    val templateGroupUid = getTemplateGroupUidResult.obj
                    observerBus.notifyNoteDataSetChanged(templateGroupUid)
                }
            }

            addTemplatesResult
        }
}