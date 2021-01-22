package com.ivanovsky.passnotes.util

import android.content.Context
import androidx.lifecycle.LifecycleOwner

fun Context.getLifecycleOwner(): LifecycleOwner? {
    return (this as? LifecycleOwner)
}