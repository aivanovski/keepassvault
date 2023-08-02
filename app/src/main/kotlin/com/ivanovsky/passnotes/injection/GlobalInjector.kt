package com.ivanovsky.passnotes.injection

import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersHolder

object GlobalInjector {

    inline fun <reified T : Any> inject(): Lazy<T> =
        GlobalContext.get().inject(
            qualifier = null,
            mode = LazyThreadSafetyMode.PUBLICATION,
            parameters = null
        )

    inline fun <reified T : Any> get(
        params: ParametersHolder? = null
    ): T = GlobalContext.get().get(
        qualifier = null,
        parameters = if (params != null) {
            { params }
        } else {
            null
        }
    )
}