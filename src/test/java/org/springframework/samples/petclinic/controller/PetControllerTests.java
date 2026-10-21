/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.entity.Owner;
import org.springframework.samples.petclinic.entity.Pet;
import org.springframework.samples.petclinic.entity.PetType;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.repository.PetTypeRepository;
import org.springframework.samples.petclinic.service.PetService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Test class for the {@link PetController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(PetController.class)
@Import(PetService.class)
@DisabledInNativeImage
@DisabledInAotMode
class PetControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository types;

	private Owner owner;

	@BeforeEach
	void setup() {
		PetType hamster = new PetType();
		hamster.setId(3);
		hamster.setName("hamster");
		given(this.types.findById(3)).willReturn(Optional.of(hamster));

		this.owner = new Owner();
		this.owner.setId(TEST_OWNER_ID);
		Pet pet = new Pet();
		Pet dog = new Pet();
		this.owner.addPet(pet);
		this.owner.addPet(dog);
		pet.setId(TEST_PET_ID);
		dog.setId(TEST_PET_ID + 1);
		pet.setName("petty");
		pet.setType(hamster);
		pet.setBirthDate(LocalDate.of(2015, 2, 12));
		dog.setName("doggy");
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(this.owner));
		given(this.owners.saveAndFlush(any(Owner.class))).willAnswer(invocation -> {
			Owner saved = invocation.getArgument(0);
			saved.getPets().stream().filter(Pet::isNew).forEach(newPet -> newPet.setId(42));
			return saved;
		});
	}

	private ResultActions postPet(String json) throws Exception {
		return mockMvc.perform(post("/api/owners/{ownerId}/pets", TEST_OWNER_ID).contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private ResultActions putPet(int petId, String json) throws Exception {
		return mockMvc.perform(
				put("/api/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, petId).contentType(MediaType.APPLICATION_JSON)
					.content(json));
	}

	@Test
	void showPet() throws Exception {
		mockMvc.perform(get("/api/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, TEST_PET_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("petty"))
			.andExpect(jsonPath("$.type.id").value(3))
			.andExpect(jsonPath("$.birthDate").value("2015-02-12"));
	}

	@Test
	void showPetNotFound() throws Exception {
		mockMvc.perform(get("/api/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, 999))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Pet 999 not found for owner 1"));
	}

	@Test
	void unknownOwnerIsNotFound() throws Exception {
		mockMvc.perform(post("/api/owners/{ownerId}/pets", 999).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Betty", "birthDate": "2015-02-12", "typeId": 3}""")).andExpect(status().isNotFound());
	}

	@Test
	void createPet() throws Exception {
		postPet("""
				{"name": "Betty", "birthDate": "2015-02-12", "typeId": 3}""").andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/owners/1/pets/42"))
			.andExpect(jsonPath("$.id").value(42))
			.andExpect(jsonPath("$.name").value("Betty"))
			.andExpect(jsonPath("$.type.name").value("hamster"))
			.andExpect(jsonPath("$.visits", hasSize(0)));
	}

	@Test
	void createPetWithMissingFields() throws Exception {
		postPet("{}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors", hasSize(3)))
			.andExpect(jsonPath("$.errors[?(@.field == 'name')].code").value("required"))
			.andExpect(jsonPath("$.errors[?(@.field == 'birthDate')].code").value("required"))
			.andExpect(jsonPath("$.errors[?(@.field == 'typeId')].code").value("required"));
		verify(this.owners, never()).saveAndFlush(any(Owner.class));
	}

	@Test
	void createPetWithUnknownType() throws Exception {
		postPet("""
				{"name": "Betty", "birthDate": "2015-02-12", "typeId": 77}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'typeId' && @.code == 'notFound')]").exists());
	}

	@Test
	void createPetWithNameOfExistingPetIsAllowed() throws Exception {
		postPet("""
				{"name": "Petty", "birthDate": "2015-02-12", "typeId": 3}""").andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/owners/1/pets/42"))
			.andExpect(jsonPath("$.id").value(42))
			.andExpect(jsonPath("$.name").value("Petty"));
	}

	@Test
	void createPetWithFutureBirthDate() throws Exception {
		String tomorrow = LocalDate.now().plusDays(1).toString();
		postPet("""
				{"name": "Betty", "birthDate": "%s", "typeId": 3}""".formatted(tomorrow))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'birthDate')].code").value("typeMismatch.birthDate"));
	}

	@Test
	void createPetWithInvalidDateFormat() throws Exception {
		postPet("""
				{"name": "Betty", "birthDate": "2015/02/12", "typeId": 3}""").andExpect(status().isBadRequest());
	}

	@Test
	void updatePet() throws Exception {
		putPet(TEST_PET_ID, """
				{"name": "Betty", "birthDate": "2015-02-12", "typeId": 3}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TEST_PET_ID))
			.andExpect(jsonPath("$.name").value("Betty"));
	}

	@Test
	void updatePetKeepingItsOwnName() throws Exception {
		putPet(TEST_PET_ID, """
				{"name": "petty", "birthDate": "2015-02-12", "typeId": 3}""").andExpect(status().isOk());
	}

	@Test
	void updatePetWithNameOfAnotherPetIsAllowed() throws Exception {
		putPet(TEST_PET_ID, """
				{"name": "doggy", "birthDate": "2015-02-12", "typeId": 3}""").andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TEST_PET_ID))
			.andExpect(jsonPath("$.name").value("doggy"));
	}

	@Test
	void updatePetRequiresType() throws Exception {
		putPet(TEST_PET_ID, """
				{"name": "Betty", "birthDate": "2015-02-12"}""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'typeId')].code").value("required"));
	}

	@Test
	void updateUnknownPet() throws Exception {
		putPet(999, """
				{"name": "Betty", "birthDate": "2015-02-12", "typeId": 3}""").andExpect(status().isNotFound());
	}

	@Test
	void deletePet() throws Exception {
		mockMvc.perform(delete("/api/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, TEST_PET_ID))
			.andExpect(status().isNoContent());
		assertThat(this.owner.getPet(TEST_PET_ID)).isNull();
		assertThat(this.owner.getPets()).hasSize(1);
		verify(this.owners).save(this.owner);
	}

	@Test
	void deleteUnknownPet() throws Exception {
		mockMvc.perform(delete("/api/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, 999))
			.andExpect(status().isNotFound());
		verify(this.owners, never()).save(any(Owner.class));
	}

}
