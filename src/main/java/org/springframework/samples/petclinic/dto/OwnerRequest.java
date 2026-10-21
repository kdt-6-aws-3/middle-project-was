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

package org.springframework.samples.petclinic.dto;

import org.springframework.samples.petclinic.entity.Owner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating an owner. The rules mirror {@link Owner}.
 */
public record OwnerRequest(@NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 255) String address,
		@NotBlank @Pattern(regexp = Owner.PHONE_PATTERN, message = "{phone.invalid}") String phone) {

	public void applyTo(Owner owner) {
		owner.setName(this.name);
		owner.setAddress(this.address);
		owner.setPhone(this.phone);
	}

}
