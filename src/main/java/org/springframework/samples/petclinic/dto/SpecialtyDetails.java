package org.springframework.samples.petclinic.dto;

import org.springframework.samples.petclinic.entity.Specialty;

public record SpecialtyDetails(Integer id, String name) {

	public static SpecialtyDetails from(Specialty specialty) {
		return new SpecialtyDetails(specialty.getId(), specialty.getName());
	}

}
