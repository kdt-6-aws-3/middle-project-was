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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.system.PageResponse;
import org.springframework.samples.petclinic.system.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST API for searching, creating and updating owners.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@RestController
@RequestMapping("/api/owners")
class OwnerController {

	private static final int MAX_PAGE_SIZE = 100;

	private final OwnerRepository owners;

	OwnerController(OwnerRepository owners) {
		this.owners = owners;
	}

	@GetMapping
	PageResponse<OwnerSummary> findOwners(@RequestParam(defaultValue = "") String lastName,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "5") int size) {
		// empty last name signifies broadest possible search
		int validatedPage = Math.max(page, 1);
		int validatedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
		Page<Owner> results = this.owners.findByLastNameStartingWith(lastName.strip(),
				PageRequest.of(validatedPage - 1, validatedSize));
		return PageResponse.from(results, OwnerSummary::from);
	}

	@GetMapping("/{ownerId}")
	OwnerDetails showOwner(@PathVariable int ownerId) {
		return OwnerDetails.from(findOwner(ownerId));
	}

	@PostMapping
	ResponseEntity<OwnerDetails> createOwner(@Valid @RequestBody OwnerRequest request) {
		Owner owner = new Owner();
		request.applyTo(owner);
		Owner saved = this.owners.save(owner);
		return ResponseEntity.created(URI.create("/api/owners/" + saved.getId())).body(OwnerDetails.from(saved));
	}

	@PutMapping("/{ownerId}")
	OwnerDetails updateOwner(@PathVariable int ownerId, @Valid @RequestBody OwnerRequest request) {
		Owner owner = findOwner(ownerId);
		request.applyTo(owner);
		return OwnerDetails.from(this.owners.save(owner));
	}

	private Owner findOwner(int ownerId) {
		return this.owners.findById(ownerId)
			.orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " not found"));
	}

}
