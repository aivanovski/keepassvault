package com.ivanovsky.passnotes.data.repository.encdb.dao;

import com.ivanovsky.passnotes.data.entity.Group;

import java.util.List;
import java.util.UUID;

public interface GroupDao {

	List<Group> getAll();
	UUID insert(Group group);
}
