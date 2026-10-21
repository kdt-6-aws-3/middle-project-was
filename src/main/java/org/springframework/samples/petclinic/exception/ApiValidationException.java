/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.exception;

import java.util.Map;

import org.springframework.validation.Errors;

/**
 * Thrown when business validation (rules that cannot be expressed as bean validation
 * annotations, e.g. a birth date in the future) fails. Rendered as a 400 problem detail
 * by {@link ApiExceptionHandler}.
 */
public class ApiValidationException extends RuntimeException {

	private final Errors errors;

	private final Map<String, String> fieldAliases;

	/**
	 * @param errors the collected validation errors
	 * @param fieldAliases maps entity field names to the request field names exposed by
	 * the API (e.g. {@code type} to {@code typeId})
	 */
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
