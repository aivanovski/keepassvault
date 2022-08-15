package com.ivanovsky.passnotes

import com.google.firebase.crashlytics.FirebaseCrashlytics

class GPlayApp : App() {

    override fun onCreate() {
        super.onCreate()

        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(BuildConfig.IS_CRASHLYTICS_ENABLED)
    }
}