package com.ivanovsky.passnotes

import com.ivanovsky.passnotes.injection.DIModuleBuilder

class FDroidApp : App() {

    override fun configureModuleBuilder(builder: DIModuleBuilder) {
        builder.isExternalStorageAccessEnabled = true
    }
}