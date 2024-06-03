package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottlerImpl
import com.ivanovsky.passnotes.injection.GlobalInjector

class DebugFileSystemResolver(
    override var factories: Map<FSType, Factory>
) : FileSystemResolver(factories) {

    fun setupFactories(factories: Map<FSType, Factory>) {
        this.factories = factories
    }

    class FakeFileSystemFactory : Factory {
        override fun createProvider(fsAuthority: FSAuthority): FileSystemProvider {
            val observerBus: ObserverBus = GlobalInjector.get()
            val throttler = ThreadThrottlerImpl()

            return FakeFileSystemProvider(throttler, observerBus, fsAuthority)
        }
    }
}