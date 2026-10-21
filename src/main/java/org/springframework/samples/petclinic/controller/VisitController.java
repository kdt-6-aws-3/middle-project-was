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

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.dto.VisitDetails;
import org.springframework.samples.petclinic.dto.VisitRequest;
import org.springframework.samples.petclinic.service.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST API for listing and booking the visits of a pet.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
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

}
