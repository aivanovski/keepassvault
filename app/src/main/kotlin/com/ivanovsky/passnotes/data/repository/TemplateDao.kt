package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import java.util.UUID

interface TemplateDao {
    fun getTemplateGroupUid(): OperationResult<UUID?>
    fun getTemplates(): OperationResult<List<Template>>
    fun addTemplates(templates: List<Template>): OperationResult<Boolean>
    fun addTemplates(templates: List<Template>, doInterstitialCommits: Boolean): OperationResult<Boolean>
}