package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import org.koin.dsl.module

object FileSystemProvidersModule {

    fun build(
        isExternalStorageAccessEnabled: Boolean
    ) = module {
//        val fsTypes = mutableSetOf<FSType>()
//            .apply {
//                add(FSType.INTERNAL_STORAGE)
//                if (isExternalStorageAccessEnabled) {
//                    add(FSType.EXTERNAL_STORAGE)
//                }
//                add(FSType.WEBDAV)
//                add(FSType.SAF)
//                add(FSType.GIT)
//            }

//        val defaultFactories = FileSystemResolver.createDefaultFactories()

        val factories = FileSystemResolver.buildFactories(
            isExternalStorageAccessEnabled = isExternalStorageAccessEnabled
        )

        single { FileSystemResolver(factories) }
    }
}