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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.samples.petclinic.dto.VisitDetails;
import org.springframework.samples.petclinic.dto.VisitRequest;
import org.springframework.samples.petclinic.entity.Owner;
import org.springframework.samples.petclinic.entity.Pet;
import org.springframework.samples.petclinic.entity.Visit;
import org.springframework.samples.petclinic.exception.ApiValidationException;
import org.springframework.samples.petclinic.exception.ResourceNotFoundException;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

/**
 * Listing and booking the visits of a pet. Visits are saved through the pet's owner.
 */
@Service
public class VisitService {

	private final OwnerRepository owners;

	public VisitService(OwnerRepository owners) {
		this.owners = owners;
	}

	public List<VisitDetails> listVisits(int ownerId, int petId) {
		return findPet(findOwner(ownerId), petId).getVisits().stream().map(VisitDetails::from).toList();
	}

	public VisitDetails createVisit(int ownerId, int petId, VisitRequest request) {
		Owner owner = findOwner(ownerId);
		Set<Integer> existingVisitIds = findPet(owner, petId).getVisits()
			.stream()
			.map(Visit::getId)
			.collect(Collectors.toSet());

		Visit visit = new Visit();
		visit.setDate(request.date());
		visit.setDescription(request.description());
		Errors errors = new BeanPropertyBindingResult(visit, "visit");
		if (!visit.getDate().isAfter(LocalDate.now())) {
			errors.rejectValue("date", "typeMismatch.visitDate");
			throw new ApiValidationException(errors);
		}

		owner.addVisit(petId, visit);
		Owner saved = this.owners.save(owner);
		Visit created = saved.getPet(petId)
			.getVisits()
			.stream()
			.filter(candidate -> !existingVisitIds.contains(candidate.getId()))
			.findFirst()
			.orElseThrow();
		return VisitDetails.from(created);
	}

	private Owner findOwner(int ownerId) {
		return this.owners.findById(ownerId)
			.orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " not found"));
	}

	private static Pet findPet(Owner owner, int petId) {
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new ResourceNotFoundException("Pet " + petId + " not found for owner " + owner.getId());
		}
		return pet;
	}

}
