package com.ivanovsky.passnotes.domain.biometric

import android.content.Context

class BiometricResolverImpl(
    context: Context
) : BiometricResolver {

    private val interactor = BiometricInteractorImpl(context)

    override fun getInteractor(): BiometricInteractor = interactor
}