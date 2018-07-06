package com.ivanovsky.passnotes.data.safedb.dao;

import com.ivanovsky.passnotes.data.safedb.model.Group;

import java.util.List;

public interface GroupDao {

	List<Group> getAll();
	String insert(Group group);
}
