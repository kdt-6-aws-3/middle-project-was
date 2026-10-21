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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.samples.petclinic.dto.OwnerDetails;
import org.springframework.samples.petclinic.dto.OwnerRequest;
import org.springframework.samples.petclinic.dto.OwnerSummary;
import org.springframework.samples.petclinic.dto.PageResponse;
import org.springframework.samples.petclinic.entity.Owner;
import org.springframework.samples.petclinic.entity.Pet;
import org.springframework.samples.petclinic.exception.ResourceNotFoundException;
import org.springframework.samples.petclinic.repository.AppointmentRepository;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Searching, creating, updating and deleting owners.
 */
@Service
public class OwnerService {

	private static final int MAX_PAGE_SIZE = 100;

	private final OwnerRepository owners;

	private final AppointmentRepository appointments;

	public OwnerService(OwnerRepository owners, AppointmentRepository appointments) {
		this.owners = owners;
		this.appointments = appointments;
	}

	/**
	 * Finds owners whose name starts with the given value. Page numbers are 1-based;
	 * out-of-range page and size values are clamped.
	 */
	public PageResponse<OwnerSummary> findOwners(String name, int page, int size) {
		// empty name signifies broadest possible search
		int validatedPage = Math.max(page, 1);
		int validatedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
		Page<Owner> results = this.owners.findByNameStartingWith(name.strip(),
				PageRequest.of(validatedPage - 1, validatedSize));
		return PageResponse.from(results, OwnerSummary::from);
	}

	public OwnerDetails getOwner(int ownerId) {
		return OwnerDetails.from(findOwner(ownerId));
	}

	public OwnerDetails createOwner(OwnerRequest request) {
		Owner owner = new Owner();
		request.applyTo(owner);
		return OwnerDetails.from(this.owners.save(owner));
	}

	public OwnerDetails updateOwner(int ownerId, OwnerRequest request) {
		Owner owner = findOwner(ownerId);
		request.applyTo(owner);
		return OwnerDetails.from(this.owners.save(owner));
	}

	/**
	 * Deletes the owner together with their pets, visits and appointments (the foreign
	 * keys have no ON DELETE CASCADE, so the children are deleted first).
	 */
	@Transactional
	public void deleteOwner(int ownerId) {
		Owner owner = findOwner(ownerId);
		List<Integer> petIds = owner.getPets().stream().map(Pet::getId).toList();
		if (!petIds.isEmpty()) {
			this.appointments.deleteByPetIds(petIds);
		}
		this.owners.delete(owner);
	}

	private Owner findOwner(int ownerId) {
		return this.owners.findById(ownerId)
			.orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " not found"));
	}

}
