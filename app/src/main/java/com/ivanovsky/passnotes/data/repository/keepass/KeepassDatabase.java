package com.ivanovsky.passnotes.data.repository.keepass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.TemplateRepository;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.encdb.exception.FailedToWriteDBException;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassGroupDao;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassNoteDao;
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus;
import com.ivanovsky.passnotes.domain.usecases.DetermineDatabaseStatusUseCase;
import com.ivanovsky.passnotes.util.FileUtils;
import com.ivanovsky.passnotes.util.InputOutputUtils;
import com.ivanovsky.passnotes.util.Logger;
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

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UNSUPPORTED_CONFIG_TYPE;
import static com.ivanovsky.passnotes.data.entity.OperationError.newAuthError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;

public class KeepassDatabase implements EncryptedDatabase {

    private EncryptedDatabaseKey key;
    private final FileDescriptor file;
    private final KeepassGroupRepository groupRepository;
    private final KeepassNoteRepository noteRepository;
    private final KeepassTemplateRepository templateRepository;
    private final FileSystemResolver fileSystemResolver;
    private final DetermineDatabaseStatusUseCase statusUseCase;
    private final ObserverBus observerBus;
    private final FSOptions fsOptions;
    private final SimpleDatabase db;
    private final Object lock;
    private final AtomicReference<DatabaseStatus> status;

    public KeepassDatabase(FileSystemResolver fileSystemResolver,
                           DetermineDatabaseStatusUseCase statusUseCase,
                           ObserverBus observerBus,
                           FSOptions fsOptions,
                           FileDescriptor file,
                           InputStream in,
                           OperationResult<?> inResult,
                           EncryptedDatabaseKey key) throws EncryptedDatabaseException {
        this.fileSystemResolver = fileSystemResolver;
        this.statusUseCase = statusUseCase;
        this.observerBus = observerBus;
        this.fsOptions = fsOptions;
        this.file = file;
        this.key = key;
        this.lock = new Object();
        this.db = readDatabaseFile(in, key);
        this.status = new AtomicReference<>(statusUseCase.determineStatus(fsOptions, inResult));

        KeepassNoteDao noteDao = new KeepassNoteDao(this);
        KeepassGroupDao groupDao = new KeepassGroupDao(this);

        this.groupRepository = new KeepassGroupRepository(groupDao);
        this.noteRepository = new KeepassNoteRepository(noteDao);
        this.templateRepository = new KeepassTemplateRepository(groupDao, noteDao);

        templateRepository.findTemplateNotes();
    }

    private SimpleDatabase readDatabaseFile(InputStream in,
                                            EncryptedDatabaseKey key) throws EncryptedDatabaseException {
        SimpleDatabase result;

        Credentials credentials = new KdbxCreds(key.getKey());

        synchronized (lock) {
            try {
                result = SimpleDatabase.load(credentials, in);
            } catch (IllegalStateException e) {
                Logger.printStackTrace(e);
                throw new EncryptedDatabaseException(e);

            } catch (IOException e) {
                throw new FailedToWriteDBException();

            } catch (Exception e) {
                Logger.printStackTrace(e);
                throw new EncryptedDatabaseException(e);

            } finally {
                InputOutputUtils.close(in);
            }
        }

        return result;
    }

    @NonNull
    @Override
    public Object getLock() {
        return lock;
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
            FileSystemProvider provider = fileSystemResolver.resolveProvider(file.getFsAuthority());

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
                OperationResult<OutputStream> outResult = provider.openFileForWrite(updatedFile,
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

                DatabaseStatus newStatus = statusUseCase.determineStatus(fsOptions, result);
                if (status.get() != newStatus) {
                    status.set(newStatus);
                    observerBus.notifyDatabaseStatusChanged(newStatus);
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
            if (oldKey.equals(key)) {
                key = newKey;
                result = commit();
            } else {
                result = OperationResult.error(newAuthError());
            }
        }

        return result;
    }
}
