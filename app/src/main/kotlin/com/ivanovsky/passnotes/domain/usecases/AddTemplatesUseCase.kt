package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.entity.TemplateField
import com.ivanovsky.passnotes.data.entity.TemplateFieldType
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext
import java.util.UUID

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

    private fun createDefaultTemplates(): List<Template> {
        return listOf(
            Template(
                uid = UUID.randomUUID(),
                title = "ID card",
                fields = listOf(
                    TemplateField(
                        title = "Number",
                        position = 0,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "Name",
                        position = 1,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "Place of issue",
                        position = 2,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "Date of issue",
                        position = 3,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "@exp_date",
                        position = 4,
                        type = TemplateFieldType.DATE_TIME
                    )
                )
            ),
            Template(
                uid = UUID.randomUUID(),
                title = "E-Mail",
                fields = listOf(
                    TemplateField(
                        title = "E-Mail",
                        position = 0,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "URL",
                        position = 1,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "Password",
                        position = 2,
                        type = TemplateFieldType.PROTECTED_INLINE
                    )
                )
            ),
            Template(
                uid = UUID.randomUUID(),
                title = "Wireless LAN",
                fields = listOf(
                    TemplateField(
                        title = "SSID",
                        position = 0,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "Password",
                        position = 1,
                        type = TemplateFieldType.PROTECTED_INLINE
                    )
                )
            ),
            Template(
                uid = UUID.randomUUID(),
                title = "Secure note",
                fields = listOf(
                    TemplateField(
                        title = "Notes",
                        position = 0,
                        type = TemplateFieldType.INLINE
                    )
                )
            ),
            Template(
                uid = UUID.randomUUID(),
                title = "Credit card",
                fields = listOf(
                    TemplateField(
                        title = "Number",
                        position = 0,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "CVV",
                        position = 1,
                        type = TemplateFieldType.PROTECTED_INLINE
                    ),
                    TemplateField(
                        title = "PIN",
                        position = 2,
                        type = TemplateFieldType.PROTECTED_INLINE
                    ),
                    TemplateField(
                        title = "Card holder",
                        position = 3,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "@exp_date",
                        position = 4,
                        type = TemplateFieldType.DATE_TIME
                    )
                )
            ),
            Template(
                uid = UUID.randomUUID(),
                title = "Membership",
                fields = listOf(
                    TemplateField(
                        title = "Number",
                        position = 0,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "URL",
                        position = 1,
                        type = TemplateFieldType.INLINE
                    ),
                    TemplateField(
                        title = "@exp_date",
                        position = 2,
                        type = TemplateFieldType.DATE_TIME
                    )
                )
            )
        )
    }
}