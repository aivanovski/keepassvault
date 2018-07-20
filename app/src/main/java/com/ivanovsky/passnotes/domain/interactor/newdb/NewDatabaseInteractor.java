package com.ivanovsky.passnotes.domain.interactor.newdb;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.injection.Injector;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class NewDatabaseInteractor {

	private final EncryptedDatabaseRepository dbRepository;
	private final UsedFileRepository usedFileRepository;
	private final ObserverBus observerBus;

	public NewDatabaseInteractor(EncryptedDatabaseRepository dbRepository,
								 UsedFileRepository usedFileRepository,
								 ObserverBus observerBus) {
		this.dbRepository = dbRepository;
		this.usedFileRepository = usedFileRepository;
		this.observerBus = observerBus;
	}

	public Observable<Boolean> createNewDatabaseAndOpen(KeepassDatabaseKey key, File file) {
		return Observable.fromCallable(() -> makeAndOpen(key, file))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread());
	}

	private Boolean makeAndOpen(KeepassDatabaseKey key, File file) throws EncryptedDatabaseException {
		boolean result = false;

		if (!file.exists()
				&& dbRepository.createNew(key, FileDescriptor.fromRegularFile(file))) {

			UsedFile usedFile = new UsedFile();
			usedFile.setFilePath(file.getPath());
			usedFile.setLastAccessTime(System.currentTimeMillis());
			usedFileRepository.insert(usedFile);

			observerBus.notifyUsedFileDataSetChanged();

			EncryptedDatabase db = dbRepository.open(key, FileDescriptor.fromRegularFile(file));
			if (db != null) {
				Injector.getInstance().createEncryptedDatabaseComponent(db);

				result = true;
			}
		}

		return result;
	}
}
