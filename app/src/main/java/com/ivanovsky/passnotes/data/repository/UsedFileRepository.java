package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.FSAuthority;
import com.ivanovsky.passnotes.data.repository.db.AppDatabase;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.db.dao.UsedFileDao;

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

    @Nullable
    public UsedFile findByUid(String fileUid, FSAuthority fsAuthority) {
        return Stream.of(dao.getAll())
                .filter(file -> fileUid.equals(file.getFileUid()) && fsAuthority.equals(file.getFsAuthority()))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public UsedFile getById(int id) {
        return dao.getById(id);
    }

    @NonNull
    public UsedFile insert(UsedFile file) {
        long id = dao.insert(file);

        bus.notifyUsedFileDataSetChanged();

        return file.copy((int) id, file.getFsAuthority(), file.getFilePath(),
                file.getFileUid(), file.getAddedTime(), file.getLastAccessTime());
    }

    public void update(UsedFile file) {
        dao.update(file);

        bus.notifyUsedFileContentChanged(file.getId());
    }
}
