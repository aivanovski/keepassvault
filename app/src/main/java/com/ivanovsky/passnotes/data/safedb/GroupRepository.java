package com.ivanovsky.passnotes.data.safedb;

import com.ivanovsky.passnotes.data.safedb.model.Group;

import java.util.List;

import io.reactivex.Single;

public interface GroupRepository {

	Single<List<Group>> getAllGroup();
	void insert(Group group);
	boolean isTitleFree(String title);
}
