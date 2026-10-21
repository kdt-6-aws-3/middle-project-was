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
package org.springframework.samples.petclinic.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.dto.OwnerDetails;
import org.springframework.samples.petclinic.dto.OwnerRequest;
import org.springframework.samples.petclinic.dto.OwnerSummary;
import org.springframework.samples.petclinic.dto.PageResponse;
import org.springframework.samples.petclinic.service.OwnerService;
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

	private final OwnerService ownerService;

	OwnerController(OwnerService ownerService) {
		this.ownerService = ownerService;
	}

	@GetMapping
	PageResponse<OwnerSummary> findOwners(@RequestParam(defaultValue = "") String lastName,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "5") int size) {
		return this.ownerService.findOwners(lastName, page, size);
	}

	@GetMapping("/{ownerId}")
	OwnerDetails showOwner(@PathVariable int ownerId) {
		return this.ownerService.getOwner(ownerId);
	}

	@PostMapping
	ResponseEntity<OwnerDetails> createOwner(@Valid @RequestBody OwnerRequest request) {
		OwnerDetails created = this.ownerService.createOwner(request);
		return ResponseEntity.created(URI.create("/api/owners/" + created.id())).body(created);
	}

	@PutMapping("/{ownerId}")
	OwnerDetails updateOwner(@PathVariable int ownerId, @Valid @RequestBody OwnerRequest request) {
		return this.ownerService.updateOwner(ownerId, request);
	}

}
