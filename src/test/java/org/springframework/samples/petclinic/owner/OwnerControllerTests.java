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

package org.springframework.samples.petclinic.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test class for {@link OwnerController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(OwnerController.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final String VALID_OWNER = """
			{"firstName": "Joe", "lastName": "Bloggs", "address": "123 Caramel Street",
			 "city": "London", "telephone": "1316761638"}""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	private Owner george() {
		Owner george = new Owner();
		george.setId(TEST_OWNER_ID);
		george.setFirstName("George");
		george.setLastName("Franklin");
		george.setAddress("110 W. Liberty St.");
		george.setCity("Madison");
		george.setTelephone("6085551023");
		Pet max = new Pet();
		PetType dog = new PetType();
		dog.setId(2);
		dog.setName("dog");
		max.setType(dog);
		max.setName("Max");
		max.setBirthDate(LocalDate.of(2020, 1, 1));
		george.addPet(max);
		max.setId(1);
		Visit visit = new Visit();
		visit.setId(1);
		visit.setDate(LocalDate.of(2024, 5, 1));
		visit.setDescription("rabies shot");
		max.addVisit(visit);
		return george;
	}

	@BeforeEach
	void setup() {
		Owner george = george();
		given(this.owners.findByLastNameStartingWith(eq("Franklin"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george), PageRequest.of(0, 5), 1));
		given(this.owners.findByLastNameStartingWith(eq(""), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george, new Owner()), PageRequest.of(0, 5), 2));
		given(this.owners.findByLastNameStartingWith(eq("Unknown"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0));
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(george));
		given(this.owners.save(any(Owner.class))).willAnswer(invocation -> {
			Owner owner = invocation.getArgument(0);
			if (owner.isNew()) {
				owner.setId(99);
			}
			return owner;
		});
	}

	@Test
	void findOwnersByLastName() throws Exception {
		mockMvc.perform(get("/api/owners").param("lastName", "Franklin"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].lastName").value("Franklin"))
			.andExpect(jsonPath("$.content[0].pets[0]").value("Max"))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void findOwnersWithoutLastNameReturnsAll() throws Exception {
		mockMvc.perform(get("/api/owners"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void findOwnersNoResultReturnsEmptyContent() throws Exception {
		mockMvc.perform(get("/api/owners").param("lastName", "Unknown"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(0)));
	}

	@Test
	void findOwnersClampsPageAndSize() throws Exception {
		mockMvc.perform(get("/api/owners").param("page", "0").param("size", "1000")).andExpect(status().isOk());
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(this.owners).findByLastNameStartingWith(eq(""), pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isZero();
		assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
	}

	@Test
	void showOwner() throws Exception {
		mockMvc.perform(get("/api/owners/{ownerId}", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("George"))
			.andExpect(jsonPath("$.telephone").value("6085551023"))
			.andExpect(jsonPath("$.pets[0].name").value("Max"))
			.andExpect(jsonPath("$.pets[0].birthDate").value("2020-01-01"))
			.andExpect(jsonPath("$.pets[0].type.name").value("dog"))
			.andExpect(jsonPath("$.pets[0].visits[0].description").value("rabies shot"));
	}

	@Test
	void showOwnerNotFound() throws Exception {
		mockMvc.perform(get("/api/owners/{ownerId}", 99999))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("Owner 99999 not found"));
	}

	@Test
	void createOwner() throws Exception {
		mockMvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON).content(VALID_OWNER))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/owners/99"))
			.andExpect(jsonPath("$.id").value(99))
			.andExpect(jsonPath("$.lastName").value("Bloggs"))
			.andExpect(jsonPath("$.pets", hasSize(0)));
	}

	@Test
	void createOwnerWithInvalidFields() throws Exception {
		String invalid = """
				{"firstName": "Joe", "lastName": "Bloggs", "address": "", "city": "London",
				 "telephone": "12345"}""";
		mockMvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON).content(invalid))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Validation failed"))
			.andExpect(jsonPath("$.errors", hasSize(2)))
			.andExpect(jsonPath("$.errors[?(@.field == 'address')].code").value("NotBlank"))
			.andExpect(jsonPath("$.errors[?(@.field == 'telephone')].code").value("Pattern"))
			.andExpect(jsonPath("$.errors[?(@.field == 'telephone')].message")
				.value("Telephone must be a 10-digit number"));
		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void createOwnerWithMalformedJson() throws Exception {
		mockMvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON).content("{not json"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void updateOwnerKeepsPets() throws Exception {
		mockMvc
			.perform(put("/api/owners/{ownerId}", TEST_OWNER_ID).contentType(MediaType.APPLICATION_JSON)
				.content(VALID_OWNER))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TEST_OWNER_ID))
			.andExpect(jsonPath("$.lastName").value("Bloggs"))
			.andExpect(jsonPath("$.pets[0].name").value("Max"));
	}

	@Test
	void updateOwnerNotFound() throws Exception {
		mockMvc
			.perform(put("/api/owners/{ownerId}", 99999).contentType(MediaType.APPLICATION_JSON).content(VALID_OWNER))
			.andExpect(status().isNotFound());
	}

}
