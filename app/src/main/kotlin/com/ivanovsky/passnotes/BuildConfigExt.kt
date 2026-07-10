package com.ivanovsky.passnotes

@Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
fun BuildConfig.isFdroidFlavor(): Boolean =
    BuildConfig.FLAVOR == "fdroid"