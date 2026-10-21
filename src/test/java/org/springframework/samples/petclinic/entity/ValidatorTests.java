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

package org.springframework.samples.petclinic.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * @author Michael Isvy Simple test to make sure that Bean Validation is working (useful
 * when upgrading to a new version of Hibernate Validator/ Bean Validation)
 */
class ValidatorTests {

	private Validator createValidator() {
		LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
		localValidatorFactoryBean.afterPropertiesSet();
		return localValidatorFactoryBean;
	}

	private <T> ConstraintViolation<T> getOnlyViolation(Set<ConstraintViolation<T>> violations) {
		assertThat(violations).hasSize(1);
		return violations.iterator().next();
	}

	private Owner owner(String name, String phone) {
		Owner owner = new Owner();
		owner.setName(name);
		owner.setAddress("Seoul");
		owner.setPhone(phone);
		return owner;
	}

	@Test
	void shouldValidateOwner() {
		assertThat(createValidator().validate(owner("Kim", "010-1234-5678"))).isEmpty();
	}

	@Test
	void shouldNotValidateWhenNameEmpty() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);

		Set<ConstraintViolation<Owner>> constraintViolations = createValidator().validate(owner("", "010-1234-5678"));

		ConstraintViolation<Owner> violation = getOnlyViolation(constraintViolations);
		assertThat(violation.getPropertyPath()).hasToString("name");
		assertThat(violation.getMessage()).isEqualTo("must not be blank");
	}

	@Test
	void shouldNotValidateWhenPhoneHasNoHyphens() {
		Set<ConstraintViolation<Owner>> constraintViolations = createValidator().validate(owner("Kim", "01012345678"));

		ConstraintViolation<Owner> violation = getOnlyViolation(constraintViolations);
		assertThat(violation.getPropertyPath()).hasToString("phone");
	}

}
