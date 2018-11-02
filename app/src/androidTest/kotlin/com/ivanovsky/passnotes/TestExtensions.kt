package com.ivanovsky.passnotes

import java.util.*

fun dateInMillis(year: Int, month: Int, day: Int): Long {
	val cal = Calendar.getInstance()

	cal.set(year, month, day)

	return cal.timeInMillis
}
