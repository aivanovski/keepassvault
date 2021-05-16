package com.ivanovsky.passnotes.util

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T?>.setIfNeed(newValue: T?) {
    if (value != newValue) {
        value = newValue
    }
}

fun <T> MutableLiveData<T?>.reset() {
    setIfNeed(null)
}

