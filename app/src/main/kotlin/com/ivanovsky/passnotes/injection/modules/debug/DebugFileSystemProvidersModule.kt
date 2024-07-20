package com.ivanovsky.passnotes.injection.modules.debug

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.fake.DebugFileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.fake.FakeFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.fake.delay.ThreadThrottlerImpl
import com.ivanovsky.passnotes.injection.GlobalInjector
import org.koin.dsl.module

object DebugFileSystemProvidersModule {

    fun build(
        isExternalStorageAccessEnabled: Boolean,
        isFakeFileSystemEnabled: Boolean
    ) = module {
        val factories = FileSystemResolver.buildFactories(
            isExternalStorageAccessEnabled = isExternalStorageAccessEnabled
        )
            .toMutableMap()

        if (isFakeFileSystemEnabled) {
            factories[FSType.FAKE] = FileSystemResolver.Factory { fsAuthority ->
                val observerBus: ObserverBus = GlobalInjector.get()
                val throttler = ThreadThrottlerImpl()

                FakeFileSystemProvider(throttler, observerBus, fsAuthority)
            }
        }

        val resolver = DebugFileSystemResolver(factories)

        single<DebugFileSystemResolver> { resolver }
        single<FileSystemResolver> { resolver }
    }
}