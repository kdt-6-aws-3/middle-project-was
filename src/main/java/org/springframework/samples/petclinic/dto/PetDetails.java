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

import java.time.LocalDate;
import java.util.List;

import org.springframework.samples.petclinic.entity.Pet;
import org.springframework.samples.petclinic.entity.PetType;

/**
 * Pet with its type and visits (oldest first).
 */
public record PetDetails(Integer id, String name, LocalDate birthDate, PetTypeDetails type, List<VisitDetails> visits) {

	public static PetDetails from(Pet pet) {
		PetType type = pet.getType();
		return new PetDetails(pet.getId(), pet.getName(), pet.getBirthDate(),
				type != null ? PetTypeDetails.from(type) : null,
				pet.getVisits().stream().map(VisitDetails::from).toList());
	}

}
