package com.ivanovsky.passnotes.data;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.entity.FSAuthority;
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus;
import com.ivanovsky.passnotes.util.ReflectionUtils;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class ObserverBus {

    private final List<Observer> observers;
    private final Handler handler;

    public interface Observer { // flag interface
    }

    public interface GroupDataSetObserver extends Observer {
        void onGroupDataSetChanged();
    }

    public interface UsedFileDataSetObserver extends Observer {
        void onUsedFileDataSetChanged();
    }

    public interface UsedFileContentObserver extends Observer {
        void onUsedFileContentChanged(int usedFileId);
    }

    public interface NoteDataSetChanged extends Observer {
        void onNoteDataSetChanged(@NonNull UUID groupUid);
    }

    public interface NoteContentObserver extends Observer {
        void onNoteContentChanged(
                @NonNull UUID groupUid, @NonNull UUID oldNoteUid, @NonNull UUID newNoteUid);
    }

    public interface DatabaseCloseObserver extends Observer {
        void onDatabaseClosed();
    }

    public interface DatabaseOpenObserver extends Observer {
        void onDatabaseOpened(@NonNull FSOptions fsOptions, @NonNull DatabaseStatus status);
    }

    public interface DatabaseStatusObserver extends Observer {
        void onDatabaseStatusChanged(@NonNull DatabaseStatus status);
    }

    public interface SyncProgressStatusObserver extends Observer {
        void onSyncProgressStatusChanged(
                @NonNull FSAuthority fsAuthority,
                @NonNull String uid,
                @NonNull SyncProgressStatus status);
    }

    public ObserverBus() {
        observers = new CopyOnWriteArrayList<>();
        handler = new Handler(Looper.getMainLooper());
    }

    public void register(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregister(Observer observer) {
        if (observers.contains(observer)) {
            observers.remove(observer);
        }
    }

    public void notifyGroupDataSetChanged() {
        for (GroupDataSetObserver observer : filterObservers(GroupDataSetObserver.class)) {
            handler.post(observer::onGroupDataSetChanged);
        }
    }

    public void notifyUsedFileDataSetChanged() {
        for (UsedFileDataSetObserver observer : filterObservers(UsedFileDataSetObserver.class)) {
            handler.post(observer::onUsedFileDataSetChanged);
        }
    }

    public void notifyUsedFileContentChanged(int usedFileId) {
        for (UsedFileContentObserver observer : filterObservers(UsedFileContentObserver.class)) {
            handler.post(() -> observer.onUsedFileContentChanged(usedFileId));
        }
    }

    public void notifyNoteDataSetChanged(UUID groupUid) {
        for (NoteDataSetChanged observer : filterObservers(NoteDataSetChanged.class)) {
            handler.post(() -> observer.onNoteDataSetChanged(groupUid));
        }
    }

    public void notifyNoteContentChanged(UUID groupUid, UUID oldNoteUid, UUID newNoteUid) {
        for (NoteContentObserver observer : filterObservers(NoteContentObserver.class)) {
            handler.post(() -> observer.onNoteContentChanged(groupUid, oldNoteUid, newNoteUid));
        }
    }

    public void notifyDatabaseClosed() {
        for (DatabaseCloseObserver observer : filterObservers(DatabaseCloseObserver.class)) {
            handler.post(observer::onDatabaseClosed);
        }
    }

    public void notifyDatabaseOpened(FSOptions fsOptions, DatabaseStatus status) {
        for (DatabaseOpenObserver observer : filterObservers(DatabaseOpenObserver.class)) {
            handler.post(() -> observer.onDatabaseOpened(fsOptions, status));
        }
    }

    public void notifyDatabaseStatusChanged(DatabaseStatus status) {
        for (DatabaseStatusObserver observer : filterObservers(DatabaseStatusObserver.class)) {
            handler.post(() -> observer.onDatabaseStatusChanged(status));
        }
    }

    public void notifySyncProgressStatusChanged(
            @NonNull FSAuthority fsAuthority,
            @NonNull String uid,
            @NonNull SyncProgressStatus status) {
        for (SyncProgressStatusObserver observer :
                filterObservers(SyncProgressStatusObserver.class)) {
            handler.post(() -> observer.onSyncProgressStatusChanged(fsAuthority, uid, status));
        }
    }

    public <T extends Observer> boolean hasObserver(Class<T> type) {
        return filterObservers(type).size() != 0;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> filterObservers(Class<T> type) {
        return Stream.of(observers)
                .filter(
                        observer ->
                                ReflectionUtils.containsInterfaceInClass(observer.getClass(), type))
                .map(observer -> (T) observer)
                .collect(Collectors.toList());
    }
}
