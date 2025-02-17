package com.ivanovsky.passnotes.presentation.core.extensions

import android.os.Build
import android.os.Bundle
import java.io.Serializable
import kotlin.reflect.KClass

@Suppress("DEPRECATION", "UNCHECKED_CAST")
fun <T : Serializable> Bundle.getSerializableCompat(
    key: String,
    type: KClass<T>
): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, type.java)
    } else {
        getSerializable(key) as? T
    }
}