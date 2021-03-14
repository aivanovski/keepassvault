package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorPresenter;
import com.ivanovsky.passnotes.presentation.note_editor.view.NoteEditorDataTransformer;

import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = { AppModule.class })
@Singleton
public interface AppComponent {

	void inject(@NotNull NoteEditorPresenter noteEditorPresenter);
    void inject(@NotNull NoteEditorDataTransformer noteEditorDataTransformer);
}
