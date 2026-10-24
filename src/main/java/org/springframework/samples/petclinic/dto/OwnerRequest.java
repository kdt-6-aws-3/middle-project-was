package org.springframework.samples.petclinic.dto;

import org.springframework.samples.petclinic.entity.Owner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerRequest(@NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 255) String address,
		@NotBlank @Pattern(regexp = Owner.PHONE_PATTERN, message = "{phone.invalid}") String phone) {

	public void applyTo(Owner owner) {
		owner.setName(this.name);
		owner.setAddress(this.address);
		owner.setPhone(this.phone);
	}

}
