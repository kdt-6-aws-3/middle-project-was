package org.springframework.samples.petclinic.dto;

import java.time.LocalDate;

public record PetRequest(String name, LocalDate birthDate, Integer typeId) {
}
