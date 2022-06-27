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
            val getDbResult = dbRepo.encryptedDatabase
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            val addTemplatesResult = db.templateDao.addTemplates(createDefaultTemplates())

            if (addTemplatesResult.isSucceededOrDeferred) {
                observerBus.notifyGroupDataSetChanged()

                val getTemplateGroupUidResult = db.templateDao.getTemplateGroupUid()
                if (getTemplateGroupUidResult.isSucceededOrDeferred) {
                    val templateGroupUid = getTemplateGroupUidResult.obj
                    observerBus.notifyNoteDataSetChanged(templateGroupUid)
                }
            }

            addTemplatesResult
        }
}