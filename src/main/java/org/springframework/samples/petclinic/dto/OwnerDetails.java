package org.springframework.samples.petclinic.dto;

import java.util.List;

import org.springframework.samples.petclinic.entity.Owner;

public record OwnerDetails(Integer id, String name, String address, String phone, List<PetDetails> pets) {

	public static OwnerDetails from(Owner owner) {
		return new OwnerDetails(owner.getId(), owner.getName(), owner.getAddress(), owner.getPhone(),
				owner.getPets().stream().map(PetDetails::from).toList());
	}

}
