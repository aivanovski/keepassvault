package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.Group;

import java.util.List;

import io.reactivex.Single;

public interface GroupRepository {

	Single<List<Group>> getAllGroup();
	boolean insert(Group group);
	boolean isTitleFree(String title);
}
