package com.ivanovsky.passnotes

import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

val TEST_DISPATCHER_PROVIDER = DispatcherProvider(
    Main = TestCoroutineDispatcher(),
    IO = TestCoroutineDispatcher(),
    Default = TestCoroutineDispatcher()
)

class TestCoroutineDispatcher : CoroutineDispatcher() {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }
}
