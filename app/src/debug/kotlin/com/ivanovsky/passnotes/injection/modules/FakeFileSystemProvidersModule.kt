package com.ivanovsky.passnotes.injection.modules

import android.content.Context
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.repository.file.FakeFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottlerImpl
import com.ivanovsky.passnotes.injection.GlobalInjector
import org.koin.dsl.module

object FakeFileSystemProvidersModule {

    fun build(
        context: Context,
        isExternalStorageAccessEnabled: Boolean
    ) = module {
        val fsFactories = mapOf(
            FSType.FAKE to FileSystemResolver.Factory { fsAuthority ->
                val observerBus: ObserverBus = GlobalInjector.get()
                val throttler = ThreadThrottlerImpl()

                FakeFileSystemProvider(context, throttler, observerBus, fsAuthority)
            }
        )

        val fsTypes = mutableSetOf<FSType>()
            .apply {
                add(FSType.INTERNAL_STORAGE)
                if (isExternalStorageAccessEnabled) {
                    add(FSType.EXTERNAL_STORAGE)
                }
                add(FSType.WEBDAV)
                add(FSType.SAF)
                add(FSType.GIT)
                add(FSType.FAKE)
            }

        single {
            FileSystemResolver(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                fsTypes,
                fsFactories
            )
        }
    }
}