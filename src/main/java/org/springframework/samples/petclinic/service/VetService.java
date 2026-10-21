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

import org.springframework.samples.petclinic.dto.VetDetails;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.stereotype.Service;

/**
 * Veterinarians, served from the "vets" cache of {@link VetRepository}.
 */
@Service
public class VetService {

	private final VetRepository vetRepository;

	public VetService(VetRepository vetRepository) {
		this.vetRepository = vetRepository;
	}

	public List<VetDetails> listVets() {
		return this.vetRepository.findAll().stream().map(VetDetails::from).toList();
	}

}
