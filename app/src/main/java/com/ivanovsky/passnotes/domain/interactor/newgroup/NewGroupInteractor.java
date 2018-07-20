package com.ivanovsky.passnotes.domain.interactor.newgroup;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.repository.GroupRepository;

public class NewGroupInteractor {

	private final GroupRepository groupRepository;
	private final ObserverBus observerBus;

	public NewGroupInteractor(GroupRepository groupRepository, ObserverBus observerBus) {
		this.groupRepository = groupRepository;
		this.observerBus = observerBus;
	}


}
