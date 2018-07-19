package com.ivanovsky.passnotes.presentation.newgroup;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.presentation.core.FragmentState;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.text.TextUtils.isEmpty;

public class NewGroupPresenter implements NewGroupContract.Presenter {

	@Inject
	EncryptedDatabaseRepository dbProvider;

	@Inject
	ObserverBus observerBus;

	private final Context context;
	private final NewGroupContract.View view;
	private final CompositeDisposable disposables;

	NewGroupPresenter(Context context, NewGroupContract.View view) {
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
	public void createNewGroup(String title) {
		String trimmedTitle = title.trim();
		if (!isEmpty(trimmedTitle)) {
			view.setState(FragmentState.LOADING);

			Disposable disposable = Observable.fromCallable(() -> createGroup(trimmedTitle))
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(this::onGroupCreated);
			disposables.add(disposable);

		} else {
			view.showTitleEditTextError(context.getString(R.string.empty_field));
		}
	}

	private GroupCreationResult createGroup(String title) {
		GroupCreationResult result = new GroupCreationResult();

		GroupRepository repository = dbProvider.getDatabase().getGroupRepository();
		if (repository.isTitleFree(title)) {
			Group group = new Group();
			group.setTitle(title);

			repository.insert(group);

			observerBus.notifyGroupDataSetChanged();

			result.created = true;
		} else {
			result.created = false;
			result.error = context.getString(R.string.group_with_this_name_is_already_exist);
		}

		return result;
	}

	private void onGroupCreated(GroupCreationResult result) {
		if (result.created) {
			view.finishScreen();
		} else {
			view.showError(result.error);
		}
	}

	private static class GroupCreationResult {
		boolean created;
		String error;
	}
}
