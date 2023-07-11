package com.ivanovsky.passnotes.data.repository.file.delay

import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler.Type

class ThreadThrottlerImpl : ThreadThrottler {

    override fun delay(type: Type) = delay(type.getDelayInMillis())

    override fun delay(delayInMillis: Long) {
        Thread.sleep(delayInMillis)
    }

    private fun Type.getDelayInMillis(): Long {
        return when (this) {
            Type.SHORT_DELAY -> 500L
            Type.MEDIUM_DELAY -> 1500L
            Type.LONG_DELAY -> 3000L
        }
    }
}