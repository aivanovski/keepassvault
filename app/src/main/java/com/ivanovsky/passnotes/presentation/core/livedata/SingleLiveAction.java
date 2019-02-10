package com.ivanovsky.passnotes.presentation.core.livedata;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 * <p>
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue() or call().
 * <p>
 * Note that only one observer is going to be notified of changes.
 */
public class SingleLiveAction<T> extends MutableLiveData<T> {

	private static final String TAG = "SingleLiveEvent";

	private final AtomicBoolean mPending = new AtomicBoolean(false);

	@MainThread
	@Override
	public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {

		if (hasActiveObservers()) {
			Log.w(TAG, "Multiple observers registered but only one will be notified of changes.");
		}

		// Observe the internal MutableLiveData
		super.observe(owner, new Observer<T>() {
			@Override
			public void onChanged(@Nullable T t) {
				if (mPending.compareAndSet(true, false)) {
					observer.onChanged(t);
				}
			}
		});
	}

	@MainThread
	public void setValue(@Nullable T t) {
		mPending.set(true);
		super.setValue(t);
	}

	/**
	 * Used for cases where T is Void, to make calls cleaner.
	 */
	@MainThread
	public void call() {
		setValue(null);
	}

	@MainThread
	public void call(@Nullable T value) {
		setValue(value);
	}
}

