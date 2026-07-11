package com.ivanovsky.passnotes

enum class AppFlavor {
    FDROID,
    GOOGLE_PLAY,
    AUTOMATION;

    companion object {

        @Suppress("KotlinConstantConditions")
        fun get(): AppFlavor {
            return when (BuildConfig.FLAVOR) {
                "fdroid" -> FDROID
                "gplay" -> GOOGLE_PLAY
                "automation" -> AUTOMATION
                else -> throw IllegalStateException()
            }
        }
    }
}