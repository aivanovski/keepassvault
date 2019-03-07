package com.ivanovsky.passnotes.util

import kotlinx.coroutines.CoroutineExceptionHandler

val COROUTINE_EXCEPTION_HANDLER = CoroutineExceptionHandler { _, e ->
	e.printStackTrace()
}