package com.ivanovsky.passnotes.presentation.newgroup;

import android.content.Context;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.domain.interactor.newgroup.NewGroupInteractor;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.presentation.core.FragmentState;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static android.text.TextUtils.isEmpty;

public class NewGroupPresenter implements NewGroupContract.Presenter {

	@Inject
	NewGroupInteractor interactor;

	private final Context context;
	private final NewGroupContract.View view;
	private final CompositeDisposable disposables;

	NewGroupPresenter(Context context, NewGroupContract.View view) {
		Injector.getInstance().getEncryptedDatabaseComponent().inject(this);
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
	public void createNewGroup(String title) {
		String trimmedTitle = title.trim();

		if (!isEmpty(trimmedTitle)) {
			view.hideKeyboard();
			view.setDoneButtonVisible(false);
			view.setState(FragmentState.LOADING);

			Disposable disposable = interactor.createNewGroup(trimmedTitle)
					.subscribe(this::onCreateGroupResult);

			disposables.add(disposable);
		} else {
			view.showTitleEditTextError(context.getString(R.string.empty_field));
		}
	}

	private void onCreateGroupResult(OperationResult<Group> result) {
		if (result.getResult() != null) {
			view.finishScreen();
		} else {
			view.setDoneButtonVisible(true);
			view.showError(result.getError().getMessage());
		}
	}
}
