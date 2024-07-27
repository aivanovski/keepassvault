package com.ivanovsky.passnotes.data.repository.file.fake.delay

interface ThreadThrottler {

    fun delay(type: Type)
    fun delay(delayInMillis: Long)

    enum class Type {
        SHORT_DELAY,
        MEDIUM_DELAY,
        LONG_DELAY
    }
}