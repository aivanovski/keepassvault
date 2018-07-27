package com.ivanovsky.passnotes.presentation.core.validation;

import android.content.res.Resources;
import android.support.annotation.StringRes;
import android.widget.EditText;

import com.annimon.stream.function.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PatternValidation extends BaseValidation {

	private final Builder builder;

	private PatternValidation(Builder builder) {
		super(builder.abortOnError, builder.priority);
		this.builder = builder;
	}

	@Override
	public boolean validateFields() {
		return validateAndSetError(builder.editTexts, createErrorMatcher(), getErrorMessage());
	}

	private String getErrorMessage() {
		Resources res = getResourcesFromEditText(builder.editTexts);
		return res.getString(builder.errorMessageId);
	}

	private Predicate<EditText> createErrorMatcher() {
		return editText -> {
			String text = editText.getText().toString();
			return !builder.pattern.matcher(text).matches();
		};
	}

	public static class Builder {

		boolean abortOnError;
		int priority;
		int errorMessageId;
		Pattern pattern;
		final List<EditText> editTexts;

		public Builder() {
			editTexts = new ArrayList<>();
		}

		public Builder withTarget(EditText editText) {
			editTexts.add(editText);
			return this;
		}

		public Builder withErrorMessage(@StringRes int messageResId) {
			this.errorMessageId = messageResId;
			return this;
		}

		public Builder withPattern(Pattern pattern) {
			this.pattern = pattern;
			return this;
		}

		public Builder withPriority(int priority) {
			this.priority = priority;
			return this;
		}

		public Builder abortOnError(boolean abortOnError) {
			this.abortOnError = abortOnError;
			return this;
		}

		public PatternValidation build() {
			return new PatternValidation(this);
		}
	}
}
