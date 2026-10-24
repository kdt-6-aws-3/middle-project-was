package org.springframework.samples.petclinic.exception;

import java.util.Map;

import org.springframework.validation.Errors;

public class ApiValidationException extends RuntimeException {

	private final Errors errors;

	private final Map<String, String> fieldAliases;

	public ApiValidationException(Errors errors, Map<String, String> fieldAliases) {
		super("Validation failed");
		this.errors = errors;
		this.fieldAliases = fieldAliases;
	}

	public ApiValidationException(Errors errors) {
		this(errors, Map.of());
	}

	public Errors getErrors() {
		return this.errors;
	}

	public Map<String, String> getFieldAliases() {
		return this.fieldAliases;
	}

}
