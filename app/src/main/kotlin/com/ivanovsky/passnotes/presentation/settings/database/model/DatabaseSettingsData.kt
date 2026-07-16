package com.ivanovsky.passnotes.presentation.settings.database.model

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig

data class DatabaseSettingsData(
    val config: EncryptedDatabaseConfig,
    val groups: List<Group>
)