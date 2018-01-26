package com.ivanovsky.passnotes.data.safedb.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.ivanovsky.passnotes.data.safedb.model.Notepad;

import java.util.List;

@Dao
public interface NotepadDao {

	@Query("SELECT * FROM notepad")
	List<Notepad> getAll();

	@Insert
	void insert(Notepad notepad);
}
