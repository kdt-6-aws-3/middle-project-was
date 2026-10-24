package org.springframework.samples.petclinic.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.samples.petclinic.entity.Pet;
import org.springframework.samples.petclinic.entity.PetType;

public record PetDetails(Integer id, String name, LocalDate birthDate, PetTypeDetails type, List<VisitDetails> visits) {

	public static PetDetails from(Pet pet) {
		PetType type = pet.getType();
		return new PetDetails(pet.getId(), pet.getName(), pet.getBirthDate(),
				type != null ? PetTypeDetails.from(type) : null,
				pet.getVisits().stream().map(VisitDetails::from).toList());
	}

}
