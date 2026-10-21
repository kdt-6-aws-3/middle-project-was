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

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.system.ApiValidationException;
import org.springframework.samples.petclinic.system.ResourceNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for adding and editing the pets of an owner.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@RestController
@RequestMapping("/api/owners/{ownerId}/pets")
class PetController {

	/**
	 * Errors are collected on {@link Pet}, whose {@code type} is {@code typeId} in the
	 * API.
	 */
	private static final Map<String, String> FIELD_ALIASES = Map.of("type", "typeId");

	private final OwnerRepository owners;

	private final PetTypeRepository types;

	PetController(OwnerRepository owners, PetTypeRepository types) {
		this.owners = owners;
		this.types = types;
	}

	@GetMapping("/{petId}")
	PetDetails showPet(@PathVariable int ownerId, @PathVariable int petId) {
		return PetDetails.from(findPet(findOwner(ownerId), petId));
	}

	@PostMapping
	ResponseEntity<PetDetails> createPet(@PathVariable int ownerId, @RequestBody PetRequest request) {
		Owner owner = findOwner(ownerId);
		Pet pet = new Pet();
		throwIfInvalid(applyAndValidate(request, pet));

		// pet names may repeat within an owner, so the new pet is found by its new id
		Set<Integer> existingPetIds = owner.getPets().stream().map(Pet::getId).collect(Collectors.toSet());
		owner.addPet(pet);
		Owner saved = this.owners.saveAndFlush(owner);
		Pet created = saved.getPets()
			.stream()
			.filter(candidate -> !existingPetIds.contains(candidate.getId()))
			.findFirst()
			.orElseThrow();
		return ResponseEntity.created(URI.create("/api/owners/" + ownerId + "/pets/" + created.getId()))
			.body(PetDetails.from(created));
	}

	@PutMapping("/{petId}")
	PetDetails updatePet(@PathVariable int ownerId, @PathVariable int petId, @RequestBody PetRequest request) {
		Owner owner = findOwner(ownerId);
		Pet existingPet = findPet(owner, petId);
		Pet pet = new Pet();
		pet.setId(petId);
		throwIfInvalid(applyAndValidate(request, pet));

		existingPet.setName(pet.getName());
		existingPet.setBirthDate(pet.getBirthDate());
		existingPet.setType(pet.getType());
		Owner saved = this.owners.saveAndFlush(owner);
		return PetDetails.from(saved.getPet(petId));
	}

	/**
	 * Copies the request onto the pet and runs the original PetClinic pet rules.
	 */
	private Errors applyAndValidate(PetRequest request, Pet pet) {
		pet.setName(request.name());
		pet.setBirthDate(request.birthDate());
		Errors errors = new BeanPropertyBindingResult(pet, "pet");
		if (request.typeId() != null) {
			this.types.findById(request.typeId())
				.ifPresentOrElse(pet::setType, () -> errors.rejectValue("type", "notFound", "not found"));
		}
		new PetValidator().validate(pet, errors);
		if (request.typeId() == null && !pet.isNew()) {
			// PetValidator only requires a type for new pets; the API always requires it
			errors.rejectValue("type", "required", "required");
		}
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(LocalDate.now())) {
			errors.rejectValue("birthDate", "typeMismatch.birthDate");
		}
		return errors;
	}

	private void throwIfInvalid(Errors errors) {
		if (errors.hasErrors()) {
			throw new ApiValidationException(errors, FIELD_ALIASES);
		}
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
