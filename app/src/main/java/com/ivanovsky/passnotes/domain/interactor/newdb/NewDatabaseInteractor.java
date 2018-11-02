package com.ivanovsky.passnotes.domain.interactor.newdb;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.util.Logger;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.ivanovsky.passnotes.data.entity.OperationError.newFileIsAlreadyExistsError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericError;

public class NewDatabaseInteractor {

	private final EncryptedDatabaseRepository dbRepository;
	private final UsedFileRepository usedFileRepository;
	private final ObserverBus observerBus;
	private final FileSystemResolver fileSystemResolver;

	public NewDatabaseInteractor(EncryptedDatabaseRepository dbRepository,
								 UsedFileRepository usedFileRepository,
								 FileSystemResolver fileSystemResolver,
								 ObserverBus observerBus) {
		this.dbRepository = dbRepository;
		this.usedFileRepository = usedFileRepository;
		this.observerBus = observerBus;
		this.fileSystemResolver = fileSystemResolver;
	}

	public Single<OperationResult<Boolean>> createNewDatabaseAndOpen(KeepassDatabaseKey key,
																	 FileDescriptor file) {
		return Single.fromCallable(() -> createNewDatabaseAndOpenAsync(key, file))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread());
	}

	private OperationResult<Boolean> createNewDatabaseAndOpenAsync(KeepassDatabaseKey key,
																   FileDescriptor file) {
		OperationResult<Boolean> result = new OperationResult<>();

		FileSystemProvider provider = fileSystemResolver.resolveProvider(file.getFsType());
		if (!provider.exists(file)) {
			if (dbRepository.createNew(key, file)) {
				UsedFile usedFile = new UsedFile();

				usedFile.setFilePath(file.getPath());
				usedFile.setFileUid(file.getUid());
				usedFile.setFsType(file.getFsType());
				usedFile.setLastAccessTime(System.currentTimeMillis());

				usedFileRepository.insert(usedFile);

				observerBus.notifyUsedFileDataSetChanged();

				try {
					EncryptedDatabase db = dbRepository.open(key, file);

					Injector.getInstance().createEncryptedDatabaseComponent(db);

					result.setResult(true);
				} catch (EncryptedDatabaseException e) {
					Logger.printStackTrace(e);

					result.setError(newGenericError(OperationError.MESSAGE_UNKNOWN_ERROR, e));
				}
			} else {
				result.setError(newGenericError(OperationError.MESSAGE_UNKNOWN_ERROR));
			}
		} else {
			result.setError(newFileIsAlreadyExistsError());
		}

		return result;
	}
}
