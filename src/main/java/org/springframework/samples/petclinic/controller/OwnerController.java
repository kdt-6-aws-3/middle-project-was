package org.springframework.samples.petclinic.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.dto.OwnerDetails;
import org.springframework.samples.petclinic.dto.OwnerRequest;
import org.springframework.samples.petclinic.dto.OwnerSummary;
import org.springframework.samples.petclinic.dto.PageResponse;
import org.springframework.samples.petclinic.service.OwnerService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/owners")
class OwnerController {

	private final OwnerService ownerService;

	OwnerController(OwnerService ownerService) {
		this.ownerService = ownerService;
	}

	@GetMapping
	PageResponse<OwnerSummary> findOwners(@RequestParam(defaultValue = "") String name,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "5") int size) {
		return this.ownerService.findOwners(name, page, size);
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

	@DeleteMapping("/{ownerId}")
	ResponseEntity<Void> deleteOwner(@PathVariable int ownerId) {
		this.ownerService.deleteOwner(ownerId);
		return ResponseEntity.noContent().build();
	}

}
