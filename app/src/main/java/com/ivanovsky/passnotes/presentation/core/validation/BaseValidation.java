package com.ivanovsky.passnotes.presentation.core.validation;

import android.content.res.Resources;
import android.widget.EditText;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;

import java.util.List;
import java.util.Set;

import static com.ivanovsky.passnotes.util.CollectionUtils.getFirstOrNull;

public abstract class BaseValidation {

	public static final int PRIORITY_MAX = 3;
	public static final int PRIORITY_NORMAL = 2;
	public static final int PRIORITY_MIN = 1;

	private final boolean abortOnError;
	private final int priority;

	abstract boolean validateFields();

	BaseValidation(boolean abortOnError, int priority) {
		this.abortOnError = abortOnError;
		this.priority = priority;
	}

	boolean isAbortOnError() {
		return abortOnError;
	}

	int getPriority() {
		return priority;
	}

	Resources getResourcesFromEditText(List<EditText> editTexts) {
		Resources result = null;

		EditText firstEditText = getFirstOrNull(editTexts);
		if (firstEditText != null) {
			result = firstEditText.getContext().getResources();
		}

		return result;
	}

	boolean validateAndSetError(List<EditText> editTexts, Predicate<EditText> matcher, String errorMessage) {
		boolean result = true;

		Set<EditText> matchedEditTexts = Stream.of(editTexts)
				.filter(matcher)
				.collect(Collectors.toSet());

		for (EditText editText : editTexts) {
			if (matchedEditTexts.contains(editText)) {
				result = false;
				editText.setError(errorMessage);
			} else {
				editText.setError(null);
			}
		}

		return result;
	}
}
