package org.springframework.samples.petclinic.controller;

import java.util.List;

import org.springframework.samples.petclinic.dto.VetDetails;
import org.springframework.samples.petclinic.service.VetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class VetController {

	private final VetService vetService;

	VetController(VetService vetService) {
		this.vetService = vetService;
	}

	@GetMapping("/api/vets")
	List<VetDetails> listVets() {
		return this.vetService.listVets();
	}

}
