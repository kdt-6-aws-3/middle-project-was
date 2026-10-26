package org.springframework.samples.petclinic.controller;

import java.net.URI;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.dto.AppointmentDetails;
import org.springframework.samples.petclinic.dto.AppointmentRequest;
import org.springframework.samples.petclinic.dto.AppointmentStatusRequest;
import org.springframework.samples.petclinic.dto.PageResponse;
import org.springframework.samples.petclinic.entity.AppointmentStatus;
import org.springframework.samples.petclinic.service.AppointmentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
class AppointmentController {

	private final AppointmentService appointmentService;

	AppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@GetMapping
	PageResponse<AppointmentDetails> findAppointments(@RequestParam(required = false) Integer hospitalId,
			@RequestParam(required = false) Integer vetId, @RequestParam(required = false) Integer petId,
			@RequestParam(required = false) AppointmentStatus status, @RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return this.appointmentService.findAppointments(hospitalId, vetId, petId, status, page, size);
	}

	@GetMapping("/{appointmentId}")
	AppointmentDetails showAppointment(@PathVariable int appointmentId) {
		return this.appointmentService.getAppointment(appointmentId);
	}

	@PostMapping
	ResponseEntity<AppointmentDetails> createAppointment(@Valid @RequestBody AppointmentRequest request) {
		AppointmentDetails created = this.appointmentService.createAppointment(request);
		return ResponseEntity.created(URI.create("/api/appointments/" + created.id())).body(created);
	}

	@PutMapping("/{appointmentId}")
	AppointmentDetails updateAppointment(@PathVariable int appointmentId,
			@Valid @RequestBody AppointmentRequest request) {
		return this.appointmentService.updateAppointment(appointmentId, request);
	}

	@PatchMapping("/{appointmentId}/status")
	AppointmentDetails changeStatus(@PathVariable int appointmentId,
			@Valid @RequestBody AppointmentStatusRequest request) {
		return this.appointmentService.changeStatus(appointmentId, request.status());
	}

	@DeleteMapping
	Map<String, Integer> deleteAllAppointments() {
		return Map.of("deleted", this.appointmentService.deleteAllAppointments());
	}

	@DeleteMapping("/{appointmentId}")
	ResponseEntity<Void> deleteAppointment(@PathVariable int appointmentId) {
		this.appointmentService.deleteAppointment(appointmentId);
		return ResponseEntity.noContent().build();
	}

}
