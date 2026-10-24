package org.springframework.samples.petclinic.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppointmentRequest(@NotNull Integer hospitalId, @NotNull Integer petId, @NotNull Integer vetId,
		@NotNull LocalDateTime appointmentAt, @Size(max = 255) String reason) {
}
