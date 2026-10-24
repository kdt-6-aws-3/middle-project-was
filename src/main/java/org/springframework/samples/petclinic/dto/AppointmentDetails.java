package org.springframework.samples.petclinic.dto;

import java.time.LocalDateTime;

import org.springframework.samples.petclinic.entity.Appointment;
import org.springframework.samples.petclinic.entity.AppointmentStatus;

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
