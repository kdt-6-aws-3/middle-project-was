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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.samples.petclinic.dto.PetDetails;
import org.springframework.samples.petclinic.dto.PetRequest;
import org.springframework.samples.petclinic.entity.Owner;
import org.springframework.samples.petclinic.entity.Pet;
import org.springframework.samples.petclinic.exception.ApiValidationException;
import org.springframework.samples.petclinic.exception.ResourceNotFoundException;
import org.springframework.samples.petclinic.repository.AppointmentRepository;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.repository.PetTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

/**
 * Adding, editing and deleting the pets of an owner. Pets are saved through their owner.
 */
@Service
public class PetService {

	/**
	 * Errors are collected on {@link Pet}, whose {@code type} is {@code typeId} in the
	 * API.
	 */
	private static final Map<String, String> FIELD_ALIASES = Map.of("type", "typeId");

	private final OwnerRepository owners;

	private final PetTypeRepository types;

	private final AppointmentRepository appointments;

	public PetService(OwnerRepository owners, PetTypeRepository types, AppointmentRepository appointments) {
		this.owners = owners;
		this.types = types;
		this.appointments = appointments;
	}

	public PetDetails getPet(int ownerId, int petId) {
		return PetDetails.from(findPet(findOwner(ownerId), petId));
	}

	public PetDetails createPet(int ownerId, PetRequest request) {
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
		return PetDetails.from(created);
	}

	public PetDetails updatePet(int ownerId, int petId, PetRequest request) {
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
	 * Deletes the pet together with its visits and appointments.
	 */
	@Transactional
	public void deletePet(int ownerId, int petId) {
		Owner owner = findOwner(ownerId);
		Pet pet = findPet(owner, petId);
		this.appointments.deleteByPetIds(List.of(petId));
		owner.removePet(pet);
		this.owners.save(owner);
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
