package org.springframework.samples.petclinic.dto;

import java.util.List;

import org.springframework.samples.petclinic.entity.Hospital;
import org.springframework.samples.petclinic.entity.Vet;

public record VetDetails(Integer id, String name, HospitalDetails hospital, List<SpecialtyDetails> specialties) {

	public static VetDetails from(Vet vet) {
		Hospital hospital = vet.getHospital();
		return new VetDetails(vet.getId(), vet.getName(), hospital != null ? HospitalDetails.from(hospital) : null,
				vet.getSpecialties().stream().map(SpecialtyDetails::from).toList());
	}

}
