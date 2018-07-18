package com.ivanovsky.passnotes.data.keepass;

import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.safedb.GroupRepository;
import com.ivanovsky.passnotes.data.safedb.dao.GroupDao;
import com.ivanovsky.passnotes.data.safedb.model.Group;

import java.util.List;
import java.util.UUID;

import io.reactivex.Single;

public class KeepassGroupRepository implements GroupRepository {

	private final GroupDao dao;

	KeepassGroupRepository(GroupDao dao) {
		this.dao = dao;
	}

	@Override
	public Single<List<Group>> getAllGroup() {
		return Single.fromCallable(dao::getAll);
	}

	@Override
	public boolean isTitleFree(String title) {
		boolean result;

		synchronized (this) {
			result = !Stream.of(dao.getAll())
					.anyMatch(group -> title.equals(group.getTitle()));
		}

		return result;
	}

	@Override
	public boolean insert(Group group) {
		boolean result = false;

		synchronized (this) {
			UUID uid = dao.insert(group);
			if (uid != null) {
				group.setUid(uid);
				result = true;
			}
		}

		return result;
	}
}
