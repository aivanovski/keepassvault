package com.ivanovsky.passnotes.util;

import android.support.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

public class CollectionUtils {

	public static <T> T getFirstOrNull(@NonNull List<T> items) {
		T result = null;

		if (items.size() != 0) {
			result = items.get(0);
		}

		return result;
	}

	public static <T> List<T> newLinkedListWith(T item) {
		List<T> result = new LinkedList<>();
		result.add(item);
		return result;
	}
}
