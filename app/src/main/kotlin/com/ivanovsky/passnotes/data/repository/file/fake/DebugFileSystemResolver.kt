package com.ivanovsky.passnotes.data.repository.file.fake

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.fake.delay.ThreadThrottlerImpl
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.injection.GlobalInjector.get

class DebugFileSystemResolver(
    override var factories: Map<FSType, Factory>
) : FileSystemResolver(factories) {

    fun setupFactories(factories: Map<FSType, Factory>) {
        this.factories = factories
    }

    class FakeFileSystemFactory : Factory {
        override fun createProvider(fsAuthority: FSAuthority): FileSystemProvider {
            val fsResolver: FileSystemResolver = get()
            val observerBus: ObserverBus = get()
            val resourceProvider: ResourceProvider = get()
            val throttler = ThreadThrottlerImpl()

            return FakeFileSystemProvider(
                fsResolver = fsResolver,
                observerBus = observerBus,
                resourceProvider = resourceProvider,
                throttler = throttler,
                fsAuthority = fsAuthority
            )
        }
    }
}