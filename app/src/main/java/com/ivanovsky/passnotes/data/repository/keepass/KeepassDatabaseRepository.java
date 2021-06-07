package com.ivanovsky.passnotes.data.repository.keepass;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.TemplateRepository;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor;
import com.ivanovsky.passnotes.util.Logger;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;

public class KeepassDatabaseRepository implements EncryptedDatabaseRepository {

    private static final String DEFAULT_DB_PATH = "default.kdbx";
    private static final String DEFAULT_DB_PASSWORD = "123";

    private volatile KeepassDatabase db;
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
        // TODO: should be rewritten with wrapper
        return db.getNoteRepository();
    }

    @Override
    public GroupRepository getGroupRepository() {
        // TODO: should be rewritten with wrapper
        return db.getGroupRepository();
    }

    @Override
    public TemplateRepository getTemplateRepository() {
        return db.getTemplateRepository();
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

            OperationResult<InputStream> inResult = fsProvider.openFileForRead(file,
                    OnConflictStrategy.CANCEL,
                    options);
            if (inResult.isFailed()) {
                return inResult.takeError();
            }

            InputStream in = inResult.getObj();

            try {
                db = new KeepassDatabase(fileSystemResolver, options, file, in, key.getKey());
                result = inResult.takeStatusWith(db);
            } catch (EncryptedDatabaseException e) {
                Logger.printStackTrace(e);
                result = OperationResult.error(newDbError(e.getMessage()));
            }
        }

        onDatabaseOpened();

        return result;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> createNew(@NonNull EncryptedDatabaseKey key,
                                              @NonNull FileDescriptor file) {
        OperationResult<Boolean> result = new OperationResult<>();

        synchronized (lock) {
            Credentials defaultCredentials = new KdbxCreds(DEFAULT_DB_PASSWORD.getBytes());
            Credentials newCredentials = new KdbxCreds(key.getKey());

            InputStream in = null;
            OutputStream out = null;

            FileSystemProvider provider = fileSystemResolver.resolveProvider(file.getFsAuthority());

            try {
                in = new BufferedInputStream(context.getAssets().open(DEFAULT_DB_PATH));

                SimpleDatabase keepassDb = SimpleDatabase.load(defaultCredentials, in);

                OperationResult<OutputStream> outResult = provider.openFileForWrite(file,
                        OnConflictStrategy.CANCEL,
                        FSOptions.defaultOptions());
                if (outResult.isSucceededOrDeferred()) {
                    out = outResult.getObj();

                    keepassDb.save(newCredentials, out);

                    result.setObj(true);
                } else {
                    result.setError(outResult.getError());
                }
            } catch (Exception e) {
                Logger.printStackTrace(e);

            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // TODO: should be handled or not?
                        Logger.printStackTrace(e);
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // TODO: should be handled or not?
                        Logger.printStackTrace(e);
                    }
                }
            }
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

    private void onDatabaseOpened() {
        lockInteractor.onDatabaseOpened();
        observerBus.notifyDatabaseOpened();
    }

    private void onDatabaseClosed() {
        lockInteractor.onDatabaseClosed();
        observerBus.notifyDatabaseClosed();
    }
}
