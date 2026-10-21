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

import java.util.List;

import org.springframework.samples.petclinic.entity.Hospital;
import org.springframework.samples.petclinic.entity.Vet;

/**
 * Veterinarian with the hospital they work at and specialties sorted by name.
 */
public record VetDetails(Integer id, String name, HospitalDetails hospital, List<SpecialtyDetails> specialties) {

	public static VetDetails from(Vet vet) {
		Hospital hospital = vet.getHospital();
		return new VetDetails(vet.getId(), vet.getName(), hospital != null ? HospitalDetails.from(hospital) : null,
				vet.getSpecialties().stream().map(SpecialtyDetails::from).toList());
	}

}
