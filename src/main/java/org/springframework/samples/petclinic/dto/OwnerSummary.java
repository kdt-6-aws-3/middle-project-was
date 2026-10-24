package org.springframework.samples.petclinic.dto;

import java.util.List;

import org.springframework.samples.petclinic.entity.Owner;
import org.springframework.samples.petclinic.entity.Pet;

public record OwnerSummary(Integer id, String name, String address, String phone, List<String> pets) {

	public static OwnerSummary from(Owner owner) {
		return new OwnerSummary(owner.getId(), owner.getName(), owner.getAddress(), owner.getPhone(),
				owner.getPets().stream().map(Pet::getName).toList());
	}

}
