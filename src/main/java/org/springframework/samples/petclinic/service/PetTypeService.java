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
package org.springframework.samples.petclinic.service;

import java.util.List;

import org.springframework.samples.petclinic.dto.PetTypeDetails;
import org.springframework.samples.petclinic.repository.PetTypeRepository;
import org.springframework.stereotype.Service;

/**
 * The pet types a pet can be registered with.
 */
@Service
public class PetTypeService {

	private final PetTypeRepository types;

	public PetTypeService(PetTypeRepository types) {
		this.types = types;
	}

	public List<PetTypeDetails> listPetTypes() {
		return this.types.findPetTypes().stream().map(PetTypeDetails::from).toList();
	}

}
