package com.ivanovsky.passnotes.injection.encdb;

import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;

import dagger.Module;
import dagger.Provides;

@Module
public class EncryptedDatabaseModule {

	private final EncryptedDatabase database;

	public EncryptedDatabaseModule(EncryptedDatabase database) {
		this.database = database;
	}

	@Provides
	@EncryptedDatabaseScope
	public NoteRepository provideNoteRepository() {
		return database.getNoteRepository();
	}

	@Provides
	@EncryptedDatabaseScope
	public GroupRepository provideGroupRepository() {
		return database.getGroupRepository();
	}
}
