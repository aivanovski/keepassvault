package com.ivanovsky.passnotes.ui.core.validation;

import android.content.res.Resources;
import android.support.annotation.StringRes;
import android.widget.EditText;

import com.annimon.stream.function.Predicate;

import java.util.ArrayList;
import java.util.List;

public class NotEmptyValidation extends BaseValidation {

	private Builder builder;

	private NotEmptyValidation(Builder builder) {
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
			return text.isEmpty() && text.trim().isEmpty();
		};
	}

	public static class Builder {

		int errorMessageId;
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

		public NotEmptyValidation build() {
			return new NotEmptyValidation(this);
		}
	}
}
