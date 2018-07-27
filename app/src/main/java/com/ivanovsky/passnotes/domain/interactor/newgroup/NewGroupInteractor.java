package com.ivanovsky.passnotes.domain.interactor.newgroup;

import android.content.Context;

import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.repository.GroupRepository;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class NewGroupInteractor {

	private final Context context;
	private final GroupRepository groupRepository;
	private final ObserverBus observerBus;

	public NewGroupInteractor(Context context, GroupRepository groupRepository, ObserverBus observerBus) {
		this.context = context;
		this.groupRepository = groupRepository;
		this.observerBus = observerBus;
	}

	public Observable<OperationResult<Group>> createNewGroup(String title) {
		return Observable.fromCallable(() -> makeGroup(title))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread());
	}

	private OperationResult<Group> makeGroup(String title) {
		OperationResult<Group> result = new OperationResult<>();

		if (isTitleFree(title)) {
			Group group = new Group();
			group.setTitle(title);

			OperationResult<Boolean> insertResult = groupRepository.insert(group);
			if (insertResult.isSuccessful()) {
				observerBus.notifyGroupDataSetChanged();
				result.setResult(group);
			} else {
				result.setError(new OperationError(OperationError.Type.GENERIC_ERROR,
						context.getString(R.string.error_was_occurred)));
			}
		} else {
			result.setError(new OperationError(OperationError.Type.GENERIC_ERROR,
					context.getString(R.string.group_with_this_name_is_already_exist)));
		}

		return result;
	}

	private boolean isTitleFree(String title) {
		OperationResult<List<Group>> groupsResult = groupRepository.getAllGroup();

		return groupsResult.isSuccessful() &&
				Stream.of(groupsResult.getResult())
				.noneMatch(group -> group.getTitle().equals(title));
	}
}
