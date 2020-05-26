package com.ivanovsky.passnotes.injection.encdb;

import android.content.Context;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.domain.ClipboardHelper;
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor;
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor;
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor;
import com.ivanovsky.passnotes.domain.interactor.note_editor.NoteEditorInteractor;
import com.ivanovsky.passnotes.domain.interactor.notes.NotesInteractor;

import dagger.Module;
import dagger.Provides;

@Module
public class EncryptedDatabaseModule {

	private final Context context;
	private final EncryptedDatabase database;

	public EncryptedDatabaseModule(Context context, EncryptedDatabase database) {
		this.context = context;
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

	@Provides
	@EncryptedDatabaseScope
	GroupInteractor provideNewGroupInteractor(GroupRepository groupRepository,
											  ObserverBus observerBus) {
		return new GroupInteractor(context, groupRepository, observerBus);
	}

	@Provides
	@EncryptedDatabaseScope
	NoteInteractor provideAddEditNoteInteractor(NoteRepository noteRepository, ClipboardHelper clipboardHelper) {
		return new NoteInteractor(noteRepository, clipboardHelper);
	}

	@Provides
	@EncryptedDatabaseScope
	NoteEditorInteractor providerNoteEditorInteractor(NoteRepository noteRepository, ObserverBus observerBus) {
		return new NoteEditorInteractor(noteRepository, observerBus);
	}
}
