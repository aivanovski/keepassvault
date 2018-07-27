package com.ivanovsky.passnotes.domain.interactor.newdb;

import android.content.Context;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.util.Logger;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class NewDatabaseInteractor {

	private final Context context;
	private final EncryptedDatabaseRepository dbRepository;
	private final UsedFileRepository usedFileRepository;
	private final ObserverBus observerBus;

	public NewDatabaseInteractor(Context context,
								 EncryptedDatabaseRepository dbRepository,
								 UsedFileRepository usedFileRepository,
								 ObserverBus observerBus) {
		this.context = context;
		this.dbRepository = dbRepository;
		this.usedFileRepository = usedFileRepository;
		this.observerBus = observerBus;
	}

	public Observable<OperationResult<Boolean>> createNewDatabaseAndOpen(KeepassDatabaseKey key, File file) {
		return Observable.fromCallable(() -> makeDatabaseAndOpen(key, file))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread());
	}

	private OperationResult<Boolean> makeDatabaseAndOpen(KeepassDatabaseKey key, File file) {
		OperationResult<Boolean> result = new OperationResult<>();

		if (!file.exists()) {
			if (dbRepository.createNew(key, FileDescriptor.fromRegularFile(file))) {
				UsedFile usedFile = new UsedFile();
				usedFile.setFilePath(file.getPath());
				usedFile.setLastAccessTime(System.currentTimeMillis());
				usedFileRepository.insert(usedFile);

				observerBus.notifyUsedFileDataSetChanged();

				try {
					EncryptedDatabase db = dbRepository.open(key, FileDescriptor.fromRegularFile(file));

					Injector.getInstance().createEncryptedDatabaseComponent(db);

					result.setResult(true);
				} catch (EncryptedDatabaseException e) {
					Logger.printStackTrace(e);

					result.setError(new OperationError(OperationError.Type.GENERIC_ERROR, e));
				}
			} else {
				result.setError(new OperationError(OperationError.Type.GENERIC_ERROR,
						context.getString(R.string.error_was_occurred)));
			}
		} else {
			result.setError(new OperationError(OperationError.Type.GENERIC_ERROR,
					context.getString(R.string.file_is_already_exist)));
		}

		return result;
	}
}
