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
import org.springframework.samples.petclinic.dto.PetDetails;
import org.springframework.samples.petclinic.dto.PetRequest;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for adding, editing and deleting the pets of an owner.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@RestController
@RequestMapping("/api/owners/{ownerId}/pets")
class PetController {

	private final PetService petService;

	PetController(PetService petService) {
		this.petService = petService;
	}

	@GetMapping("/{petId}")
	PetDetails showPet(@PathVariable int ownerId, @PathVariable int petId) {
		return this.petService.getPet(ownerId, petId);
	}

	@PostMapping
	ResponseEntity<PetDetails> createPet(@PathVariable int ownerId, @RequestBody PetRequest request) {
		PetDetails created = this.petService.createPet(ownerId, request);
		return ResponseEntity.created(URI.create("/api/owners/" + ownerId + "/pets/" + created.id())).body(created);
	}

	@PutMapping("/{petId}")
	PetDetails updatePet(@PathVariable int ownerId, @PathVariable int petId, @RequestBody PetRequest request) {
		return this.petService.updatePet(ownerId, petId, request);
	}

	@DeleteMapping("/{petId}")
	ResponseEntity<Void> deletePet(@PathVariable int ownerId, @PathVariable int petId) {
		this.petService.deletePet(ownerId, petId);
		return ResponseEntity.noContent().build();
	}

}
