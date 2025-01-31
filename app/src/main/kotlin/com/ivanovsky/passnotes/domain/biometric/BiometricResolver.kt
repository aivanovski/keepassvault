package com.ivanovsky.passnotes.domain.biometric

interface BiometricResolver {
    fun getInteractor(): BiometricInteractor
}