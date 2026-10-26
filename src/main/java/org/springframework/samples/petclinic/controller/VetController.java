package org.springframework.samples.petclinic.controller;

import org.springframework.samples.petclinic.dto.PageResponse;
import org.springframework.samples.petclinic.dto.VetDetails;
import org.springframework.samples.petclinic.service.VetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class VetController {

	private final VetService vetService;

	VetController(VetService vetService) {
		this.vetService = vetService;
	}

	@GetMapping("/api/vets")
	PageResponse<VetDetails> listVets(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.vetService.listVets(page, size);
	}

}
