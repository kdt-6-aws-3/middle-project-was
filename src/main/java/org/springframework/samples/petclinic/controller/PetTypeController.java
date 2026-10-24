package org.springframework.samples.petclinic.controller;

import java.util.List;

import org.springframework.samples.petclinic.dto.PetTypeDetails;
import org.springframework.samples.petclinic.service.PetTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PetTypeController {

	private final PetTypeService petTypeService;

	PetTypeController(PetTypeService petTypeService) {
		this.petTypeService = petTypeService;
	}

	@GetMapping("/api/pettypes")
	List<PetTypeDetails> listPetTypes() {
		return this.petTypeService.listPetTypes();
	}

}
