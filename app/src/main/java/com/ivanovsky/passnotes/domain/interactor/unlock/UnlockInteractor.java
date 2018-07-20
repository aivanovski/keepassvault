package com.ivanovsky.passnotes.domain.interactor.unlock;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.injection.Injector;

import java.io.File;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class UnlockInteractor {

	private final UsedFileRepository fileRepository;
	private final EncryptedDatabaseRepository dbRepository;

	public UnlockInteractor(UsedFileRepository fileRepository,
							EncryptedDatabaseRepository dbRepository) {
		this.fileRepository = fileRepository;
		this.dbRepository = dbRepository;
	}

	public Single<List<UsedFile>> getRecentlyOpenedFiles() {
		return fileRepository.getAllUsedFiles()
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread());
	}

	public Single<Boolean> openDatabase(KeepassDatabaseKey key, File file) {
		return dbRepository.openAsync(key, FileDescriptor.fromRegularFile(file))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.map(db -> {
					Injector.getInstance().createEncryptedDatabaseComponent(db);
					return true;
				});
	}
}
