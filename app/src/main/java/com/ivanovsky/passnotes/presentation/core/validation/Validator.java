package com.ivanovsky.passnotes.presentation.core.validation;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

public class Validator {

	private final Builder builder;

	private Validator(Builder builder) {
		this.builder = builder;
	}

	public boolean validateAll() {
		boolean result = true;

		List<BaseValidation> sortedValidations = sortValidations(builder.validations);

		for (BaseValidation validation : sortedValidations) {
			if (!validation.validateFields()) {
				result = false;

				if (validation.isAbortOnError()) {
					break;
				}
			}
		}

		return result;
	}

	public List<BaseValidation> sortValidations(List<BaseValidation> validations) {
		return Stream.of(validations)
				.sorted((v1, v2) -> (v2.getPriority() - v1.getPriority()))
				.collect(Collectors.toList());
	}

	public static class Builder {

		final List<BaseValidation> validations;

		public Builder() {
			validations = new ArrayList<>();
		}

		public Builder validation(BaseValidation validation) {
			validations.add(validation);
			return this;
		}

		public Validator build() {
			return new Validator(this);
		}
	}
}
