package com.ivanovsky.passnotes.data;

import android.os.Handler;
import android.os.Looper;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.util.ReflectionUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class ObserverBus {

    private final List<Observer> observers;
    private final Handler handler;

    public interface Observer {//flag interface
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
        void onNoteDataSetChanged(@NotNull UUID groupUid);
    }

    public interface NoteContentObserver extends Observer {
        void onNoteContentChanged(@NotNull UUID groupUid,
                                  @NotNull UUID oldNoteUid,
                                  @NotNull UUID newNoteUid);
    }

    public interface DatabaseCloseObserver extends Observer {
        void onDatabaseClosed();
    }

    public interface DatabaseOpenObserver extends Observer {
        void onDatabaseOpened();
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

    public void notifyDatabaseOpened() {
        for (DatabaseOpenObserver observer : filterObservers(DatabaseOpenObserver.class)) {
            handler.post(observer::onDatabaseOpened);
        }
    }

    public <T extends Observer> boolean hasObserver(Class<T> type) {
        return filterObservers(type).size() != 0;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> filterObservers(Class<T> type) {
        return Stream.of(observers)
                .filter(observer -> ReflectionUtils.containsInterfaceInClass(observer.getClass(), type))
                .map(observer -> (T) observer)
                .collect(Collectors.toList());
    }
}
