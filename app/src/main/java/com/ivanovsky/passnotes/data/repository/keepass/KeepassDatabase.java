package com.ivanovsky.passnotes.data.repository.keepass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.TemplateRepository;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassGroupDao;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassNoteDao;
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

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_OPEN_DB_FILE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UNSUPPORTED_CONFIG_TYPE;
import static com.ivanovsky.passnotes.data.entity.OperationError.newAuthError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;

import timber.log.Timber;

public class KeepassDatabase implements EncryptedDatabase {

    @NonNull
    private EncryptedDatabaseKey key;
    @NonNull
    private final FileDescriptor file;
    @NonNull
    private final KeepassGroupRepository groupRepository;
    @NonNull
    private final KeepassNoteRepository noteRepository;
    @NonNull
    private final KeepassTemplateRepository templateRepository;
    @NonNull
    private final FSOptions fsOptions;
    @NonNull
    private final SimpleDatabase db;
    @NonNull
    private final Object lock;
    @NonNull
    private final AtomicReference<DatabaseStatus> status;
    @NonNull
    private final FileSystemProvider fsProvider;
    @Nullable
    private final OnStatusChangeListener statusListener;

    public interface OnStatusChangeListener {
        void onDatabaseStatusChanged(DatabaseStatus status);
    }

    public static OperationResult<KeepassDatabase> open(@NonNull FileSystemProvider fsProvider,
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
                db = SimpleDatabase.load(new KdbxCreds(key.getKey()), stream);
            }
        } catch (IOException e) {
            return OperationResult.error(newGenericIOError(MESSAGE_FAILED_TO_OPEN_DB_FILE));

        } catch (Exception e) {
            Timber.d(e);
            return OperationResult.error(newDbError(MESSAGE_FAILED_TO_OPEN_DB_FILE));

        } finally {
            InputOutputUtils.close(stream);
        }

        KeepassDatabase keepassDb = new KeepassDatabase(fsProvider,
                fsOptions,
                file,
                key,
                db,
                determineDatabaseStatus(fsOptions, input),
                statusListener);

        return input.takeStatusWith(keepassDb);
    }

    @NonNull
    private static DatabaseStatus determineDatabaseStatus(@NonNull FSOptions fsOptions,
                                                          @NonNull OperationResult<?> lastOperation) {
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

    private KeepassDatabase(@NonNull FileSystemProvider fsProvider,
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

        lock = new Object();

        KeepassNoteDao noteDao = new KeepassNoteDao(this);
        KeepassGroupDao groupDao = new KeepassGroupDao(this);

        groupRepository = new KeepassGroupRepository(groupDao);
        noteRepository = new KeepassNoteRepository(noteDao);
        templateRepository = new KeepassTemplateRepository(groupDao, noteDao);

        templateRepository.findTemplateNotes();
    }

    @NonNull
    @Override
    public Object getLock() {
        return lock;
    }

    @NonNull
    @Override
    public FileDescriptor getFile() {
        return file;
    }

    @NonNull
    @Override
    public DatabaseStatus getStatus() {
        return status.get();
    }

    @NonNull
    @Override
    public OperationResult<EncryptedDatabaseConfig> getConfig() {
        KeepassDatabaseConfig config;
        synchronized (lock) {
            config = new KeepassDatabaseConfig(db.isRecycleBinEnabled());
        }

        return OperationResult.success(config);
    }

    @NonNull
    @Override
    public OperationResult<Boolean> applyConfig(@NonNull EncryptedDatabaseConfig config) {
        if (!(config instanceof KeepassDatabaseConfig)) {
            return OperationResult.error(newDbError(MESSAGE_UNSUPPORTED_CONFIG_TYPE));
        }

        OperationResult<Boolean> result;
        synchronized (lock) {
            if (db.isRecycleBinEnabled() != config.isRecycleBinEnabled()) {
                db.enableRecycleBin(config.isRecycleBinEnabled());
            }

            result = commit();
        }

        return result;
    }

    @NonNull
    @Override
    public GroupRepository getGroupRepository() {
        return groupRepository;
    }

    @NonNull
    @Override
    public NoteRepository getNoteRepository() {
        return noteRepository;
    }

    @NonNull
    @Override
    public TemplateRepository getTemplateRepository() {
        return templateRepository;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> commit() {
        OperationResult<Boolean> result = new OperationResult<>();

        synchronized (lock) {
            Credentials credentials = new KdbxCreds(key.getKey());

            FileDescriptor updatedFile = file.copy(file.getFsAuthority(),
                    file.getPath(),
                    file.getUid(),
                    FileUtils.getFileNameFromPath(file.getPath()),
                    file.isDirectory(),
                    file.isRoot(),
                    System.currentTimeMillis());

            OutputStream out = null;
            try {
                OperationResult<OutputStream> outResult = fsProvider.openFileForWrite(updatedFile,
                        OnConflictStrategy.CANCEL,
                        fsOptions);
                if (outResult.isFailed()) {
                    return outResult.takeError();
                }

                out = outResult.getObj();

                // method 'SimpleDatabase.save' closes output stream after work is done
                db.save(credentials, out);

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
    public OperationResult<Boolean> changeKey(@NonNull EncryptedDatabaseKey oldKey,
                                              @NonNull EncryptedDatabaseKey newKey) {
        OperationResult<Boolean> result;
        synchronized (lock) {
            if (oldKey.equals(key) || (oldKey instanceof DefaultDatabaseKey)) {
                key = newKey;
                result = commit();
            } else {
                result = OperationResult.error(newAuthError());
            }
        }

        return result;
    }
}
