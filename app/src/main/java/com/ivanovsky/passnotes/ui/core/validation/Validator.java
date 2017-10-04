package com.ivanovsky.passnotes.ui.core.validation;

import android.util.Pair;

import com.ivanovsky.passnotes.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ivanovsky.passnotes.util.CollectionUtils.newLinkedListWith;
import static java.util.Arrays.asList;

public class Validator {

	private final Builder builder;

	private Validator(Builder builder) {
		this.builder = builder;
	}

	public boolean validateAll() {
		boolean result = true;

		for (BaseValidation validation : builder.validations) {
			if (!validation.validateFields()) {
				result = false;
			}
		}

		return result;
	}

	public static class Builder {

		final List<Pair<ExecutionMode, List<BaseValidation>>> validations;

		public Builder() {
			validations = new ArrayList<>();
		}

		public Builder preValidation() {

		}

		public Builder nextValidation(BaseValidation validation) {
			validations.add(new Pair<>(ExecutionMode.CONSISTENTLY, newLinkedListWith(validation)))
			return this;
		}

		public Builder withExclusiveValidations(BaseValidation first, BaseValidation second) {
			validations.add(new Pair<>(ExecutionMode.EXCLUSIVE, asList(first, second)));
			return this;
		}

		public Validator build() {
			return new Validator(this);
		}
	}

	private enum ExecutionMode {
		CONSISTENTLY,
		EXCLUSIVE
	}
}
