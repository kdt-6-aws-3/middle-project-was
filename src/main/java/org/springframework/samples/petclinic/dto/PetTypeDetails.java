package org.springframework.samples.petclinic.dto;

import org.springframework.samples.petclinic.entity.PetType;

public record PetTypeDetails(Integer id, String name) {

	public static PetTypeDetails from(PetType type) {
		return new PetTypeDetails(type.getId(), type.getName());
	}

}
