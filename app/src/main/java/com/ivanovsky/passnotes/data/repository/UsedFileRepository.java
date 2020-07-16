package com.ivanovsky.passnotes.data.repository;

import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.repository.db.AppDatabase;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.db.dao.UsedFileDao;
import com.ivanovsky.passnotes.data.repository.file.FSType;

import java.util.List;

public class UsedFileRepository {

	@SuppressWarnings("unused")
	private static final String TAG = UsedFileRepository.class.getSimpleName();

	private final UsedFileDao dao;
	private final ObserverBus bus;

	public UsedFileRepository(AppDatabase db, ObserverBus bus) {
		this.dao = db.getUsedFileDao();
		this.bus = bus;
	}

	public List<UsedFile> getAll() {
		return dao.getAll();
	}

	public UsedFile findByUidAndFsType(String fileUid, FSType fsType) {
		return Stream.of(dao.getAll())
				.filter(file -> file.getFileUid().equals(fileUid) && file.getFsType() == fsType)
				.findFirst()
				.orElse(null);
	}

	public void insert(UsedFile file) {
		long id = dao.insert(file);
		file.setId((int) id);

		bus.notifyUsedFileDataSetChanged();
	}

	public void update(UsedFile file) {
		dao.update(file);

		bus.notifyUsedFileContentChanged(file.getId());
	}
}
