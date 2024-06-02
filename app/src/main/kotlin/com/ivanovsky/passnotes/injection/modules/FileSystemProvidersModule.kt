package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import org.koin.dsl.module

object FileSystemProvidersModule {

    fun build(
        isExternalStorageAccessEnabled: Boolean
    ) = module {
        val factories = FileSystemResolver.buildFactories(
            isExternalStorageAccessEnabled = isExternalStorageAccessEnabled
        )

        single { FileSystemResolver(factories) }
    }
}