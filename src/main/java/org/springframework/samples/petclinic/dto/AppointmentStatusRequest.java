package org.springframework.samples.petclinic.dto;

import org.springframework.samples.petclinic.entity.AppointmentStatus;

import jakarta.validation.constraints.NotNull;

public record AppointmentStatusRequest(@NotNull AppointmentStatus status) {
}
