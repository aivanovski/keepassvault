package com.ivanovsky.passnotes

import androidx.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.ivanovsky.passnotes.injection.KoinModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        appInstance = this

        Stetho.initializeWithDefaults(this)

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(KoinModule.appModule)
        }
    }

    companion object {

        @JvmStatic
        lateinit var appInstance: App
    }
}