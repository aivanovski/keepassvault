package com.ivanovsky.passnotes.injection.encdb;

import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor;
import com.ivanovsky.passnotes.domain.interactor.notes.NotesInteractor;

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
	NoteRepository provideNoteRepository() {
		return database.getNoteRepository();
	}

	@Provides
	@EncryptedDatabaseScope
	GroupRepository provideGroupRepository() {
		return database.getGroupRepository();
	}

	@Provides
	@EncryptedDatabaseScope
	GroupsInteractor provideGroupsInteractor(GroupRepository groupRepository,
											 NoteRepository noteRepository) {
		return new GroupsInteractor(groupRepository, noteRepository);
	}

	@Provides
	@EncryptedDatabaseScope
	NotesInteractor provideNotesInteractor(NoteRepository noteRepository) {
		return new NotesInteractor(noteRepository);
	}
}
