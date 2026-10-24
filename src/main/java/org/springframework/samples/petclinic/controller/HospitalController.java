package org.springframework.samples.petclinic.controller;

import java.util.List;

import org.springframework.samples.petclinic.dto.HospitalDetails;
import org.springframework.samples.petclinic.dto.VetDetails;
import org.springframework.samples.petclinic.service.HospitalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hospitals")
class HospitalController {

	private final HospitalService hospitalService;

	HospitalController(HospitalService hospitalService) {
		this.hospitalService = hospitalService;
	}

	@GetMapping
	List<HospitalDetails> listHospitals() {
		return this.hospitalService.listHospitals();
	}

	@GetMapping("/{hospitalId}")
	HospitalDetails showHospital(@PathVariable int hospitalId) {
		return this.hospitalService.getHospital(hospitalId);
	}

	@GetMapping("/{hospitalId}/vets")
	List<VetDetails> listVets(@PathVariable int hospitalId) {
		return this.hospitalService.listVets(hospitalId);
	}

}
