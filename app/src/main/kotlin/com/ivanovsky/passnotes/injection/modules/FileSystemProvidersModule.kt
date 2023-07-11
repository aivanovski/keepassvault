package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import org.koin.dsl.module

object FileSystemProvidersModule {

    fun build() = module {
        single { FileSystemResolver(get(), get(), get(), get(), get(), get(), get()) }
    }
}