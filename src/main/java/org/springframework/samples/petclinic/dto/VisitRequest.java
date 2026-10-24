package org.springframework.samples.petclinic.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VisitRequest(@NotNull LocalDate date, @NotBlank @Size(max = 255) String description,
		@Size(max = 255) String prescription) {
}
