package com.ivanovsky.passnotes.data.safedb.dao;

import com.ivanovsky.passnotes.data.safedb.model.Group;

import java.util.List;
import java.util.UUID;

public interface GroupDao {

	List<Group> getAll();
	UUID insert(Group group);
}
