package com.ivanovsky.passnotes.data.crypto.keyprovider

import javax.crypto.SecretKey

interface SecretKeyProvider {
    fun getSecretKey(isCreateIfNeed: Boolean): SecretKey?
}