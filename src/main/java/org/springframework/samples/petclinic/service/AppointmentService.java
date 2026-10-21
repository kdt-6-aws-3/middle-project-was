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

package org.springframework.samples.petclinic.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.samples.petclinic.dto.AppointmentDetails;
import org.springframework.samples.petclinic.dto.AppointmentRequest;
import org.springframework.samples.petclinic.entity.Appointment;
import org.springframework.samples.petclinic.entity.AppointmentStatus;
import org.springframework.samples.petclinic.exception.ApiValidationException;
import org.springframework.samples.petclinic.exception.ResourceNotFoundException;
import org.springframework.samples.petclinic.repository.AppointmentRepository;
import org.springframework.samples.petclinic.repository.HospitalRepository;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

/**
 * Booking, rescheduling and changing the status of appointments. Appointments start on
 * the hour or half hour (30-minute slots), and a vet or a pet can have only one
 * non-cancelled appointment per slot.
 */
@Service
public class AppointmentService {

	/**
	 * Errors are collected on {@link Appointment}, whose relations are ids in the API.
	 */
	private static final int SLOT_MINUTES = 30;

	private static final Map<String, String> FIELD_ALIASES = Map.of("hospital", "hospitalId", "pet", "petId", "vet",
			"vetId");

	private final AppointmentRepository appointments;

	private final HospitalRepository hospitals;

	private final PetRepository pets;

	private final VetRepository vets;

	public AppointmentService(AppointmentRepository appointments, HospitalRepository hospitals, PetRepository pets,
			VetRepository vets) {
		this.appointments = appointments;
		this.hospitals = hospitals;
		this.pets = pets;
		this.vets = vets;
	}

	public List<AppointmentDetails> findAppointments(Integer hospitalId, Integer vetId, Integer petId,
			AppointmentStatus status) {
		return this.appointments.search(hospitalId, vetId, petId, status)
			.stream()
			.map(AppointmentDetails::from)
			.toList();
	}

	public AppointmentDetails getAppointment(int appointmentId) {
		return AppointmentDetails.from(findAppointment(appointmentId));
	}

	/**
	 * Books a new appointment; it starts as {@link AppointmentStatus#REQUESTED}.
	 */
	public AppointmentDetails createAppointment(AppointmentRequest request) {
		Appointment appointment = new Appointment();
		throwIfInvalid(applyAndValidate(request, appointment));
		appointment.setStatus(AppointmentStatus.REQUESTED);
		return AppointmentDetails.from(this.appointments.save(appointment));
	}

	/**
	 * Changes hospital, pet, vet, time or reason. Cancelled and completed appointments
	 * cannot be changed.
	 */
	public AppointmentDetails updateAppointment(int appointmentId, AppointmentRequest request) {
		Appointment appointment = findAppointment(appointmentId);
		throwIfClosed(appointment);
		throwIfInvalid(applyAndValidate(request, appointment));
		return AppointmentDetails.from(this.appointments.save(appointment));
	}

	/**
	 * Moves the appointment to another status. Cancelled and completed appointments are
	 * final.
	 */
	public AppointmentDetails changeStatus(int appointmentId, AppointmentStatus status) {
		Appointment appointment = findAppointment(appointmentId);
		throwIfClosed(appointment);
		appointment.setStatus(status);
		return AppointmentDetails.from(this.appointments.save(appointment));
	}

	public void deleteAppointment(int appointmentId) {
		this.appointments.delete(findAppointment(appointmentId));
	}

	private Errors applyAndValidate(AppointmentRequest request, Appointment appointment) {
		Errors errors = new BeanPropertyBindingResult(appointment, "appointment");
		appointment.setHospital(null);
		appointment.setPet(null);
		appointment.setVet(null);
		this.hospitals.findById(request.hospitalId())
			.ifPresentOrElse(appointment::setHospital, () -> errors.rejectValue("hospital", "notFound"));
		this.pets.findById(request.petId())
			.ifPresentOrElse(appointment::setPet, () -> errors.rejectValue("pet", "notFound"));
		this.vets.findById(request.vetId())
			.ifPresentOrElse(appointment::setVet, () -> errors.rejectValue("vet", "notFound"));
		if (appointment.getHospital() != null && appointment.getVet() != null
				&& !appointment.getHospital().getId().equals(appointment.getVet().getHospital().getId())) {
			errors.rejectValue("vet", "appointment.vetNotInHospital");
		}
		appointment.setAppointmentAt(request.appointmentAt());
		appointment.setReason(request.reason());
		LocalDateTime at = request.appointmentAt();
		if (!at.isAfter(LocalDateTime.now())) {
			errors.rejectValue("appointmentAt", "appointment.notInFuture");
		}
		if (at.getMinute() % SLOT_MINUTES != 0 || at.getSecond() != 0 || at.getNano() != 0) {
			errors.rejectValue("appointmentAt", "appointment.notOnSlot");
		}
		else {
			// friendly check; the unique indexes in the schema also stop concurrent
			// bookings
			if (appointment.getVet() != null
					&& this.appointments.isVetBooked(appointment.getVet().getId(), at, appointment.getId())) {
				errors.rejectValue("appointmentAt", "appointment.vetTimeTaken");
			}
			if (appointment.getPet() != null
					&& this.appointments.isPetBooked(appointment.getPet().getId(), at, appointment.getId())) {
				errors.rejectValue("appointmentAt", "appointment.petTimeTaken");
			}
		}
		return errors;
	}

	private static void throwIfClosed(Appointment appointment) {
		if (appointment.getStatus().isClosed()) {
			Errors errors = new BeanPropertyBindingResult(appointment, "appointment");
			errors.rejectValue("status", "appointment.closed");
			throw new ApiValidationException(errors);
		}
	}

	private static void throwIfInvalid(Errors errors) {
		if (errors.hasErrors()) {
			throw new ApiValidationException(errors, FIELD_ALIASES);
		}
	}

	private Appointment findAppointment(int appointmentId) {
		return this.appointments.findById(appointmentId)
			.orElseThrow(() -> new ResourceNotFoundException("Appointment " + appointmentId + " not found"));
	}

}
