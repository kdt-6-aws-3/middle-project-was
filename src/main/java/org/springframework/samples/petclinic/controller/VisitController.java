package org.springframework.samples.petclinic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.dto.VisitDetails;
import org.springframework.samples.petclinic.dto.VisitRequest;
import org.springframework.samples.petclinic.service.VisitService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/owners/{ownerId}/pets/{petId}/visits")
class VisitController {

	private final VisitService visitService;

	VisitController(VisitService visitService) {
		this.visitService = visitService;
	}

	@GetMapping
	List<VisitDetails> listVisits(@PathVariable int ownerId, @PathVariable int petId) {
		return this.visitService.listVisits(ownerId, petId);
	}

	@PostMapping
	ResponseEntity<VisitDetails> createVisit(@PathVariable int ownerId, @PathVariable int petId,
			@Valid @RequestBody VisitRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(this.visitService.createVisit(ownerId, petId, request));
	}

	@PutMapping("/{visitId}")
	VisitDetails updateVisit(@PathVariable int ownerId, @PathVariable int petId, @PathVariable int visitId,
			@Valid @RequestBody VisitRequest request) {
		return this.visitService.updateVisit(ownerId, petId, visitId, request);
	}

	@DeleteMapping("/{visitId}")
	ResponseEntity<Void> deleteVisit(@PathVariable int ownerId, @PathVariable int petId, @PathVariable int visitId) {
		this.visitService.deleteVisit(ownerId, petId, visitId);
		return ResponseEntity.noContent().build();
	}

}
