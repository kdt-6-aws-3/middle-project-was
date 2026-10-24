package org.springframework.samples.petclinic.service;

import org.springframework.samples.petclinic.entity.Pet;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class PetValidator implements Validator {

	private static final String REQUIRED = "required";

	private static final int MAX_NAME_LENGTH = 255;

	@Override
	public void validate(Object obj, Errors errors) {
		Pet pet = (Pet) obj;
		String name = pet.getName();

		if (!StringUtils.hasText(name)) {
			errors.rejectValue("name", REQUIRED, REQUIRED);
		}
		else if (name.length() > MAX_NAME_LENGTH) {
			errors.rejectValue("name", "size", "Name must be no more than 255 characters");
		}

		if (pet.isNew() && pet.getType() == null) {
			errors.rejectValue("type", REQUIRED, REQUIRED);
		}

		if (pet.getBirthDate() == null) {
			errors.rejectValue("birthDate", REQUIRED, REQUIRED);
		}
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return Pet.class.isAssignableFrom(clazz);
	}

}
