package com.ivanovsky.passnotes.data.repository.keepass;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_OPEN_DEFAULT_DB_FILE;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericError;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.GroupRepositoryWrapper;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepositoryWrapper;
import com.ivanovsky.passnotes.data.repository.TemplateRepository;
import com.ivanovsky.passnotes.data.repository.TemplateRepositoryWrapper;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor;
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class KeepassDatabaseRepository implements EncryptedDatabaseRepository {

    private static final String EMPTY_DB_PATH = "base.kdbx.xml";

    private volatile KeepassDatabase db;
    @NonNull
    private final TemplateRepositoryWrapper templateRepositoryWrapper;
    @NonNull
    private final GroupRepositoryWrapper groupRepositoryWrapper;
    @NonNull
    private final NoteRepositoryWrapper noteRepositoryWrapper;
    private final Context context;
    private final FileSystemResolver fileSystemResolver;
    private final DatabaseLockInteractor lockInteractor;
    private final ObserverBus observerBus;
    private final Object lock;

    public KeepassDatabaseRepository(Context context,
                                     FileSystemResolver fileSystemResolver,
                                     DatabaseLockInteractor lockInteractor,
                                     ObserverBus observerBus) {
        this.context = context;
        this.fileSystemResolver = fileSystemResolver;
        this.lockInteractor = lockInteractor;
        this.observerBus = observerBus;
        this.lock = new Object();
        templateRepositoryWrapper = new TemplateRepositoryWrapper(this);
        groupRepositoryWrapper = new GroupRepositoryWrapper(this);
        noteRepositoryWrapper = new NoteRepositoryWrapper(this);
    }

    @Override
    public boolean isOpened() {
        return db != null;
    }

    @Override
    public EncryptedDatabase getDatabase() {
        return db;
    }

    @Override
    public NoteRepository getNoteRepository() {
        return noteRepositoryWrapper;
    }

    @Override
    public GroupRepository getGroupRepository() {
        return groupRepositoryWrapper;
    }

    @NonNull
    @Override
    public TemplateRepository getTemplateRepository() {
        return templateRepositoryWrapper;
    }

    @NonNull
    @Override
    public OperationResult<EncryptedDatabase> open(@NonNull EncryptedDatabaseKey key,
                                                   @NonNull FileDescriptor file,
                                                   @NonNull FSOptions options) {
        OperationResult<EncryptedDatabase> result;

        FileSystemProvider fsProvider = fileSystemResolver.resolveProvider(file.getFsAuthority());

        synchronized (lock) {
            if (db != null) {
                close();
            }

            OperationResult<InputStream> inputResult = fsProvider.openFileForRead(file,
                    OnConflictStrategy.CANCEL,
                    options);
            if (inputResult.isFailed()) {
                return inputResult.takeError();
            }

            OperationResult<KeepassDatabase> openResult = KeepassDatabase.open(fsProvider,
                    options,
                    file,
                    inputResult,
                    key,
                    observerBus::notifyDatabaseStatusChanged);
            if (openResult.isFailed()) {
                return openResult.takeError();
            }

            db = openResult.getObj();

            result = openResult.takeStatusWith(db);
        }

        if (db != null) {
            onDatabaseOpened(options, db.getStatus());
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> createNew(@NonNull EncryptedDatabaseKey key,
                                              @NonNull FileDescriptor file,
                                              boolean addTemplates) {
        OperationResult<Boolean> result;

        synchronized (lock) {
            FileSystemProvider fsProvider = fileSystemResolver.resolveProvider(file.getFsAuthority());

            OperationResult<InputStream> input;
            try {
                // 'in' will be closed in KeepassDatabase.open()
                InputStream in = new BufferedInputStream(context.getAssets().open(EMPTY_DB_PATH));
                input = OperationResult.success(in);
            } catch (IOException e) {
                Timber.d(e);
                return OperationResult.error(newGenericError(MESSAGE_FAILED_TO_OPEN_DEFAULT_DB_FILE));
            }

            EncryptedDatabaseKey unencryptedKey = new DefaultDatabaseKey();
            OperationResult<KeepassDatabase> openResult = KeepassDatabase.open(fsProvider,
                    FSOptions.defaultOptions(),
                    file,
                    input,
                    unencryptedKey,
                    null);
            if (openResult.isFailed()) {
                return openResult.takeError();
            }

            KeepassDatabase db = openResult.getObj();
            if (addTemplates) {
                OperationResult<Boolean> addTemplateResult = db.getTemplateRepository()
                        .addTemplates(TemplateFactory.createDefaultTemplates(), false);
                if (addTemplateResult.isFailed()) {
                    return addTemplateResult.takeError();
                }
            }

            // 'changeKey' will invoke commit
            OperationResult<Boolean> changeKeyResult = db.changeKey(unencryptedKey, key);
            if (changeKeyResult.isFailed()) {
                return changeKeyResult.takeError();
            }

            result = changeKeyResult;
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> close() {
        synchronized (lock) {
            if (db != null) {
                db = null;
            }
        }

        onDatabaseClosed();

        return OperationResult.success(true);
    }

    private void onDatabaseOpened(FSOptions fsOptions, DatabaseStatus status) {
        lockInteractor.onDatabaseOpened(fsOptions, status);
        observerBus.notifyDatabaseOpened(fsOptions, status);
    }

    private void onDatabaseClosed() {
        lockInteractor.onDatabaseClosed();
        observerBus.notifyDatabaseClosed();
    }
}
