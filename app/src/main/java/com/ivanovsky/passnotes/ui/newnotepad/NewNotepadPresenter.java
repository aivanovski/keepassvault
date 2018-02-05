package com.ivanovsky.passnotes.ui.newnotepad;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.repository.NotepadRepository;
import com.ivanovsky.passnotes.data.safedb.SafeDatabaseProvider;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;
import com.ivanovsky.passnotes.event.NotepadDataSetChangedEvent;
import com.ivanovsky.passnotes.ui.core.FragmentState;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.text.TextUtils.isEmpty;

public class NewNotepadPresenter implements NewNotepadContract.Presenter {

	@Inject
	SafeDatabaseProvider dbProvider;

	private final Context context;
	private final NewNotepadContract.View view;
	private final CompositeDisposable disposables;

	NewNotepadPresenter(Context context, NewNotepadContract.View view) {
		App.getDaggerComponent().inject(this);
		this.context = context;
		this.view = view;
		this.disposables = new CompositeDisposable();
	}

	@Override
	public void start() {
		view.setState(FragmentState.DISPLAYING_DATA);
	}

	@Override
	public void stop() {
		disposables.clear();
	}

	@Override
	public void createNewNotepad(String title) {
		String trimmedTitle = title.trim();
		if (!isEmpty(trimmedTitle)) {
			view.setState(FragmentState.LOADING);

			Disposable disposable = Observable.fromCallable(() -> createNotepad(trimmedTitle))
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(this::onNotepadCreated);
			disposables.add(disposable);

		} else {
			view.showTitleEditTextError(context.getString(R.string.empty_field));
		}
	}

	private CreationResult createNotepad(String title) {
		CreationResult result = new CreationResult();

		NotepadRepository repository = dbProvider.getNotepadRepository();
		if (repository.isTitleFree(title)) {
			Notepad notepad = new Notepad();
			notepad.setTitle(title);

			repository.insert(notepad);
			EventBus.getDefault().post(new NotepadDataSetChangedEvent());

			result.created = true;
		} else {
			result.created = false;
			result.error = context.getString(R.string.notepad_with_this_name_is_already_exist);
		}

		return result;
	}

	private void onNotepadCreated(CreationResult result) {
		if (result.created) {
			view.finishScreen();
		} else {
			view.showError(result.error);
		}
	}

	private static class CreationResult {
		boolean created;
		String error;
	}
}
