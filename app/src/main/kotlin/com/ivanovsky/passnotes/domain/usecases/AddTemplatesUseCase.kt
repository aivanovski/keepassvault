package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.keepass.TemplateFactory.createDefaultTemplates
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.mapError
import kotlinx.coroutines.withContext

class AddTemplatesUseCase(
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider,
    private val observerBus: ObserverBus
) {

    suspend fun addTemplates(): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.mapError()
            }

            val db = getDbResult.obj
            val addTemplatesResult = db.templateDao.addTemplates(createDefaultTemplates())

            if (addTemplatesResult.isSucceededOrDeferred) {
                observerBus.notifyGroupDataSetChanged()

                val getTemplateGroupUidResult = db.templateDao.getTemplateGroupUid()
                if (getTemplateGroupUidResult.isFailed) {
                    return@withContext getTemplateGroupUidResult.mapError()
                }

                val templateGroupUid = getTemplateGroupUidResult.obj
                if (templateGroupUid != null) {
                    observerBus.notifyNoteDataSetChanged(templateGroupUid)
                }
            }

            addTemplatesResult
        }
}