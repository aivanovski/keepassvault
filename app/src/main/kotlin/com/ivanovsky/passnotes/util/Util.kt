package com.ivanovsky.passnotes.util

import kotlinx.coroutines.CoroutineExceptionHandler

val COROUTINE_HANDLER = CoroutineExceptionHandler { _, e ->
	e.printStackTrace()
}
