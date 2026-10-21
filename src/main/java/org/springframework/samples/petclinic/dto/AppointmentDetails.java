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

package org.springframework.samples.petclinic.dto;

import java.time.LocalDateTime;

import org.springframework.samples.petclinic.entity.Appointment;
import org.springframework.samples.petclinic.entity.AppointmentStatus;

/**
 * Appointment with the names of its hospital, pet and vet.
 */
public record AppointmentDetails(Integer id, Integer hospitalId, String hospitalName, Integer petId, String petName,
		Integer vetId, String vetName, LocalDateTime appointmentAt, AppointmentStatus status, String reason,
		LocalDateTime createdAt) {

	public static AppointmentDetails from(Appointment appointment) {
		return new AppointmentDetails(appointment.getId(), appointment.getHospital().getId(),
				appointment.getHospital().getName(), appointment.getPet().getId(), appointment.getPet().getName(),
				appointment.getVet().getId(), appointment.getVet().getName(), appointment.getAppointmentAt(),
				appointment.getStatus(), appointment.getReason(), appointment.getCreatedAt());
	}

}
