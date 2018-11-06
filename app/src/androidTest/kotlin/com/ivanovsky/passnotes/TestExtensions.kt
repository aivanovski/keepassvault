package com.ivanovsky.passnotes

import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import java.util.*

fun dateInMillis(year: Int, month: Int, day: Int): Long {
	val cal = Calendar.getInstance()

	cal.set(year, month, day, 0, 0, 0)
	cal.set(Calendar.MILLISECOND, 0)

	return cal.timeInMillis
}

fun initInMemoryDatabase(): AppDatabase {
	return Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(), AppDatabase::class.java)
			.build()
}
