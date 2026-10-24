package org.springframework.samples.petclinic.service;

import java.util.List;

import org.springframework.samples.petclinic.dto.PetTypeDetails;
import org.springframework.samples.petclinic.repository.PetTypeRepository;
import org.springframework.stereotype.Service;

@Service
public class PetTypeService {

	private final PetTypeRepository types;

	public PetTypeService(PetTypeRepository types) {
		this.types = types;
	}

	public List<PetTypeDetails> listPetTypes() {
		return this.types.findPetTypes().stream().map(PetTypeDetails::from).toList();
	}

}
