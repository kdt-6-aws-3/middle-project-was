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

@Service
public class OwnerService {

	private static final int MAX_PAGE_SIZE = 100;

	private final OwnerRepository owners;

	private final AppointmentRepository appointments;

	public OwnerService(OwnerRepository owners, AppointmentRepository appointments) {
		this.owners = owners;
		this.appointments = appointments;
	}

	public PageResponse<OwnerSummary> findOwners(String name, int page, int size) {

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
