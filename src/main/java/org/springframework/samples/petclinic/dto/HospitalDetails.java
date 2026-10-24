package org.springframework.samples.petclinic.dto;

import org.springframework.samples.petclinic.entity.Hospital;

public record HospitalDetails(Integer id, String name, String address, String phone) {

	public static HospitalDetails from(Hospital hospital) {
		return new HospitalDetails(hospital.getId(), hospital.getName(), hospital.getAddress(), hospital.getPhone());
	}

}
