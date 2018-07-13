package com.ivanovsky.passnotes.data;

import android.os.Handler;
import android.os.Looper;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.util.ReflectionUtils;

import java.util.List;
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
