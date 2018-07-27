package com.ivanovsky.passnotes.domain.interactor.unlock;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;
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

	public Single<OperationResult<List<UsedFile>>> getRecentlyOpenedFiles() {
		return Single.fromCallable(fileRepository::getAllUsedFiles)
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.map(OperationResult::success);
	}

	public Single<OperationResult<Boolean>> openDatabase(KeepassDatabaseKey key, File file) {
		return Single.fromCallable(() -> dbRepository.open(key, FileDescriptor.fromRegularFile(file)))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.map(db -> {
					Injector.getInstance().createEncryptedDatabaseComponent(db);
					return OperationResult.success(true);
				})
				.onErrorResumeNext(throwable -> Single.just(makeResultFromThrowable(throwable)));
	}

	private OperationResult<Boolean> makeResultFromThrowable(Throwable throwable) {
		return OperationResult.error(new OperationError(OperationError.Type.GENERIC_ERROR, throwable));
	}
}
