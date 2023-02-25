package com.ivanovsky.passnotes.data.repository.keepass.keepass_java;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_OPEN_DB_FILE;
import static com.ivanovsky.passnotes.data.entity.OperationError.newAuthError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.TemplateDao;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.encdb.MutableEncryptedDatabaseConfig;
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao;
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.data.repository.keepass.DefaultDatabaseKey;
import com.ivanovsky.passnotes.data.repository.keepass.TemplateDaoImpl;
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus;
import com.ivanovsky.passnotes.util.FileUtils;
import com.ivanovsky.passnotes.util.InputOutputUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;
import timber.log.Timber;

public class KeepassJavaDatabase implements EncryptedDatabase {

    @NonNull private volatile EncryptedDatabaseKey key;
    @NonNull private final FileDescriptor file;
    @NonNull private final KeepassJavaGroupDao groupDao;
    @NonNull private final KeepassJavaNoteDao noteDao;
    @NonNull private final TemplateDaoImpl templateDao;
    @NonNull private final FSOptions fsOptions;
    @NonNull private final SimpleDatabase db;
    @NonNull private final ReentrantLock lock;
    @NonNull private final AtomicReference<DatabaseStatus> status;
    @NonNull private final FileSystemProvider fsProvider;
    @Nullable private final OnStatusChangeListener statusListener;

    public interface OnStatusChangeListener {
        void onDatabaseStatusChanged(DatabaseStatus status);
    }

    public static OperationResult<KeepassJavaDatabase> open(
            @NonNull FileSystemProvider fsProvider,
            @NonNull FSOptions fsOptions,
            @NonNull FileDescriptor file,
            @NonNull OperationResult<InputStream> input,
            @NonNull EncryptedDatabaseKey key,
            @Nullable OnStatusChangeListener statusListener) {
        if (input.isFailed()) {
            return input.takeError();
        }

        InputStream stream = input.getObj();

        SimpleDatabase db;
        try {
            if (key instanceof DefaultDatabaseKey) {
                db = SimpleDatabase.loadXml(stream);
            } else {
                OperationResult<byte[]> getKeyResult = key.getKey();
                if (getKeyResult.isFailed()) {
                    return getKeyResult.takeError();
                }

                byte[] keyBytes = getKeyResult.getObj();

                db = SimpleDatabase.load(new KdbxCreds(keyBytes), stream);
            }
        } catch (Exception e) {
            Timber.d(e);

            String message;
            if (!TextUtils.isEmpty(e.getMessage())) {
                message = e.getMessage();
            } else {
                message = MESSAGE_FAILED_TO_OPEN_DB_FILE;
            }

            if (e instanceof IOException) {
                return OperationResult.error(newGenericIOError(message, e));
            } else {
                return OperationResult.error(newDbError(message, e));
            }
        } finally {
            InputOutputUtils.close(stream);
        }

        KeepassJavaDatabase keepassDb =
                new KeepassJavaDatabase(
                        fsProvider,
                        fsOptions,
                        file,
                        key,
                        db,
                        determineDatabaseStatus(fsOptions, input),
                        statusListener);

        return input.takeStatusWith(keepassDb);
    }

    @NonNull
    private static DatabaseStatus determineDatabaseStatus(
            @NonNull FSOptions fsOptions, @NonNull OperationResult<?> lastOperation) {
        if (!fsOptions.isWriteEnabled()) {
            return DatabaseStatus.READ_ONLY;
        } else if (lastOperation.isDeferred() && !fsOptions.isPostponedSyncEnabled()) {
            return DatabaseStatus.CACHED;
        } else if (lastOperation.isDeferred() && fsOptions.isPostponedSyncEnabled()) {
            return DatabaseStatus.POSTPONED_CHANGES;
        } else {
            return DatabaseStatus.NORMAL;
        }
    }

    private KeepassJavaDatabase(
            @NonNull FileSystemProvider fsProvider,
            @NonNull FSOptions fsOptions,
            @NonNull FileDescriptor file,
            @NonNull EncryptedDatabaseKey key,
            @NonNull SimpleDatabase db,
            @NonNull DatabaseStatus status,
            @Nullable OnStatusChangeListener statusListener) {
        this.fsProvider = fsProvider;
        this.fsOptions = fsOptions;
        this.file = file;
        this.key = key;
        this.db = db;
        this.status = new AtomicReference<>(status);
        this.statusListener = statusListener;

        lock = new ReentrantLock();

        noteDao = new KeepassJavaNoteDao(this);
        groupDao = new KeepassJavaGroupDao(this);
        templateDao = new TemplateDaoImpl(groupDao, noteDao);
    }

    @NonNull
    @Override
    public ReentrantLock getLock() {
        return lock;
    }

    @NonNull
    @Override
    public FileDescriptor getFile() {
        return file;
    }

    @NonNull
    @Override
    public EncryptedDatabaseKey getKey() {
        return key;
    }

    @NonNull
    @Override
    public DatabaseStatus getStatus() {
        return status.get();
    }

    @NonNull
    @Override
    public OperationResult<EncryptedDatabaseConfig> getConfig() {
        EncryptedDatabaseConfig config;

        lock.lock();
        try {
            config = new MutableEncryptedDatabaseConfig(db.isRecycleBinEnabled());
        } finally {
            lock.unlock();
        }

        return OperationResult.success(config);
    }

    @NonNull
    @Override
    public OperationResult<Boolean> applyConfig(@NonNull EncryptedDatabaseConfig config) {
        OperationResult<Boolean> result;

        lock.lock();
        try {
            if (db.isRecycleBinEnabled() != config.isRecycleBinEnabled()) {
                db.enableRecycleBin(config.isRecycleBinEnabled());
            }

            result = commit();
        } finally {
            lock.unlock();
        }

        return result;
    }

    @NonNull
    @Override
    public GroupDao getGroupDao() {
        return groupDao;
    }

    @NonNull
    @Override
    public NoteDao getNoteDao() {
        return noteDao;
    }

    @NonNull
    @Override
    public TemplateDao getTemplateDao() {
        return templateDao;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> commit() {
        OperationResult<Boolean> result = new OperationResult<>();

        lock.lock();
        try {
            OperationResult<byte[]> getKeyResult = key.getKey();
            if (getKeyResult.isFailed()) {
                return getKeyResult.takeError();
            }

            byte[] keyBytes = getKeyResult.getObj();

            FileDescriptor updatedFile =
                    file.copy(
                            file.getFsAuthority(),
                            file.getPath(),
                            file.getUid(),
                            FileUtils.getFileNameFromPath(file.getPath()),
                            file.isDirectory(),
                            file.isRoot(),
                            System.currentTimeMillis());

            OutputStream out = null;
            try {
                OperationResult<OutputStream> outResult =
                        fsProvider.openFileForWrite(
                                updatedFile, OnConflictStrategy.CANCEL, fsOptions);
                if (outResult.isFailed()) {
                    return outResult.takeError();
                }

                out = outResult.getObj();

                // method 'SimpleDatabase.save' closes output stream after work is done
                db.save(new KdbxCreds(keyBytes), out);

                if (outResult.isDeferred()) {
                    result.setDeferredObj(true);
                } else {
                    result.setObj(true);
                }

                DatabaseStatus newStatus = determineDatabaseStatus(fsOptions, result);
                if (status.get() != newStatus) {
                    status.set(newStatus);
                    if (statusListener != null) {
                        statusListener.onDatabaseStatusChanged(newStatus);
                    }
                }
            } catch (IOException e) {
                InputOutputUtils.close(out);

                result.setError(newGenericIOError(e));
            }
        } finally {
            lock.unlock();
        }

        return result;
    }

    public SimpleDatabase getKeepassDatabase() {
        return db;
    }

    @Nullable
    public SimpleGroup findGroupByUid(@NonNull UUID groupUid) {
        SimpleGroup root = db.getRootGroup();
        if (root == null) return null;

        return findGroupByUid(groupUid, root);
    }

    @Nullable
    public SimpleGroup findGroupByUid(@NonNull UUID groupUid, @NonNull SimpleGroup root) {
        if (groupUid.equals(root.getUuid())) return root;

        LinkedList<SimpleGroup> nextGroups = new LinkedList<>(root.getGroups());
        SimpleGroup currentGroup;
        while ((currentGroup = nextGroups.pollFirst()) != null) {
            if (groupUid.equals(currentGroup.getUuid())) {
                return currentGroup;
            }
            nextGroups.addAll(currentGroup.getGroups());
        }

        return null;
    }

    @NonNull
    public List<SimpleGroup> getAllGroupsFromTree(@NonNull SimpleGroup root) {
        List<SimpleGroup> result = new ArrayList<>();
        result.add(root);

        LinkedList<SimpleGroup> nextGroups = new LinkedList<>(root.getGroups());

        SimpleGroup currentGroup;
        while ((currentGroup = nextGroups.pollFirst()) != null) {
            result.add(currentGroup);
            nextGroups.addAll(currentGroup.getGroups());
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> changeKey(
            @NonNull EncryptedDatabaseKey oldKey, @NonNull EncryptedDatabaseKey newKey) {
        OperationResult<Boolean> result;
        lock.lock();
        try {
            if (oldKey.equals(key) || (oldKey instanceof DefaultDatabaseKey)) {
                key = newKey;
                result = commit();
            } else {
                result = OperationResult.error(newAuthError());
            }
        } finally {
            lock.unlock();
        }

        return result;
    }
}
