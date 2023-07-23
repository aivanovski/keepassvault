package com.ivanovsky.passnotes.injection

import org.koin.core.module.Module

interface DIModuleBuilder {
    var isExternalStorageAccessEnabled: Boolean
    fun buildModules(): List<Module>
}