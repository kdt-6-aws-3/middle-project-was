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
