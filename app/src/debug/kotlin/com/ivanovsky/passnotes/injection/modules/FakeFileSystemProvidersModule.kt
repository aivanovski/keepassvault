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

    fun build(context: Context) = module {
        val fsFactories = mapOf(
            FSType.FAKE to FileSystemResolver.Factory { fsAuthority ->
                val observerBus: ObserverBus = GlobalInjector.get()
                val throttler = ThreadThrottlerImpl()

                FakeFileSystemProvider(context, throttler, observerBus, fsAuthority)
            }
        )

        single { FileSystemResolver(get(), get(), get(), get(), get(), get(), get(), fsFactories) }
    }
}