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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.dto.AppointmentDetails;
import org.springframework.samples.petclinic.dto.AppointmentRequest;
import org.springframework.samples.petclinic.dto.AppointmentStatusRequest;
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

/**
 * REST API for booking appointments and managing their status.
 */
@RestController
@RequestMapping("/api/appointments")
class AppointmentController {

	private final AppointmentService appointmentService;

	AppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@GetMapping
	List<AppointmentDetails> findAppointments(@RequestParam(required = false) Integer hospitalId,
			@RequestParam(required = false) Integer vetId, @RequestParam(required = false) Integer petId,
			@RequestParam(required = false) AppointmentStatus status) {
		return this.appointmentService.findAppointments(hospitalId, vetId, petId, status);
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
