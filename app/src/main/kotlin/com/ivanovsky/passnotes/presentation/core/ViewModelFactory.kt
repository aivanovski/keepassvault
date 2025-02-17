package com.ivanovsky.passnotes.presentation.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.parameter.parametersOf

class ViewModelFactory(
    private vararg val injectedArguments: Any
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val parameters: ParametersDefinition = {
            parametersOf(*injectedArguments)
        }

        return GlobalContext.get().get(
            clazz = modelClass.kotlin,
            qualifier = null,
            parameters = parameters
        ) as T
    }
}