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
package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.system.ApiValidationException;
import org.springframework.samples.petclinic.system.ResourceNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST API for listing and booking the visits of a pet.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@RestController
@RequestMapping("/api/owners/{ownerId}/pets/{petId}/visits")
class VisitController {

	private final OwnerRepository owners;

	VisitController(OwnerRepository owners) {
		this.owners = owners;
	}

	@GetMapping
	List<VisitDetails> listVisits(@PathVariable int ownerId, @PathVariable int petId) {
		return findPet(findOwner(ownerId), petId).getVisits().stream().map(VisitDetails::from).toList();
	}

	@PostMapping
	ResponseEntity<VisitDetails> createVisit(@PathVariable int ownerId, @PathVariable int petId,
			@Valid @RequestBody VisitRequest request) {
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
		return ResponseEntity.status(HttpStatus.CREATED).body(VisitDetails.from(created));
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
