package com.ivanovsky.passnotes.presentation.core.validation;

import android.content.res.Resources;
import android.support.annotation.StringRes;
import android.widget.EditText;

import com.annimon.stream.function.Predicate;

import static com.ivanovsky.passnotes.util.CollectionUtils.newLinkedListWith;

public class IdenticalContentValidation extends BaseValidation {

	private final Builder builder;

	private IdenticalContentValidation(Builder builder) {
		super(builder.abortOnError, builder.priority);
		this.builder = builder;
	}

	@Override
	boolean validateFields() {
		return validateAndSetError(newLinkedListWith(builder.secondEditText),
				createErrorMatcher(), getErrorMessage());
	}

	private String getErrorMessage() {
		Resources res = getResourcesFromEditText(newLinkedListWith(builder.firstEditText));
		return res.getString(builder.errorMessageId);
	}

	private Predicate<EditText> createErrorMatcher() {
		return editText -> {
			String firstText = builder.firstEditText.getText().toString();
			String secondText = builder.secondEditText.getText().toString();
			return !firstText.equals(secondText);
		};
	}

	public static class Builder {

		boolean abortOnError;
		int priority;
		int errorMessageId;
		EditText firstEditText;
		EditText secondEditText;

		public Builder() {
		}

		public Builder abortOnError(boolean abortOnError) {
			this.abortOnError = abortOnError;
			return this;
		}

		public Builder withPriority(int priority) {
			this.priority = priority;
			return this;
		}

		public Builder withErrorMessage(@StringRes int errorMessageId) {
			this.errorMessageId = errorMessageId;
			return this;
		}

		public Builder withFirstTarget(EditText firstEditText) {
			this.firstEditText = firstEditText;
			return this;
		}

		public Builder withSecondTarget(EditText secondEditText) {
			this.secondEditText = secondEditText;
			return this;
		}

		public IdenticalContentValidation build() {
			return new IdenticalContentValidation(this);
		}
	}
}
