package org.springframework.samples.petclinic.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.samples.petclinic.dto.AppointmentDetails;
import org.springframework.samples.petclinic.service.AppointmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class VetAppointmentController {

	private final AppointmentService appointmentService;

	VetAppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@GetMapping("/api/vets/{vetId}/appointments")
	List<AppointmentDetails> vetSchedule(@PathVariable int vetId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return this.appointmentService.getVetSchedule(vetId, date);
	}

}
