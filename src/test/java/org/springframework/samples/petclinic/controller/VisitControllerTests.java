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
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.samples.petclinic.entity.Visit;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.service.VisitService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Test class for {@link VisitController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(VisitController.class)
@Import(VisitService.class)
@DisabledInNativeImage
@DisabledInAotMode
class VisitControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	private static final int TEST_VISIT_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	private Owner owner;

	@BeforeEach
	void init() {
		this.owner = new Owner();
		this.owner.setId(TEST_OWNER_ID);
		Pet pet = new Pet();
		this.owner.addPet(pet);
		pet.setId(TEST_PET_ID);
		pet.setName("Leo");
		Visit visit = new Visit();
		visit.setId(TEST_VISIT_ID);
		visit.setDate(LocalDate.of(2024, 5, 1));
		visit.setDescription("rabies shot");
		visit.setPrescription("Rabisin");
		pet.addVisit(visit);
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(this.owner));
		given(this.owners.save(any(Owner.class))).willAnswer(invocation -> {
			Owner saved = invocation.getArgument(0);
			saved.getPet(TEST_PET_ID).getVisits().stream().filter(Visit::isNew).forEach(v -> v.setId(7));
			return saved;
		});
	}

	private ResultActions postVisit(String json) throws Exception {
		return mockMvc.perform(post("/api/owners/{ownerId}/pets/{petId}/visits", TEST_OWNER_ID, TEST_PET_ID)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private ResultActions putVisit(int visitId, String json) throws Exception {
		return mockMvc
			.perform(put("/api/owners/{ownerId}/pets/{petId}/visits/{visitId}", TEST_OWNER_ID, TEST_PET_ID, visitId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	@Test
	void listVisits() throws Exception {
		mockMvc.perform(get("/api/owners/{ownerId}/pets/{petId}/visits", TEST_OWNER_ID, TEST_PET_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].date").value("2024-05-01"))
			.andExpect(jsonPath("$[0].description").value("rabies shot"))
			.andExpect(jsonPath("$[0].prescription").value("Rabisin"));
	}

	@Test
	void listVisitsOfUnknownPet() throws Exception {
		mockMvc.perform(get("/api/owners/{ownerId}/pets/{petId}/visits", TEST_OWNER_ID, 999))
			.andExpect(status().isNotFound());
	}

	@Test
	void createVisit() throws Exception {
		String today = LocalDate.now().toString();
		postVisit("""
				{"date": "%s", "description": "Visit Description"}""".formatted(today)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(7))
			.andExpect(jsonPath("$.date").value(today))
			.andExpect(jsonPath("$.description").value("Visit Description"))
			.andExpect(jsonPath("$.prescription").value(nullValue()));
	}

	@Test
	void createVisitWithPrescription() throws Exception {
		String today = LocalDate.now().toString();
		postVisit("""
				{"date": "%s", "description": "otitis externa", "prescription": " Surolan "}""".formatted(today))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.prescription").value("Surolan"));
	}

	@Test
	void blankPrescriptionMeansNothingPrescribed() throws Exception {
		String today = LocalDate.now().toString();
		postVisit("""
				{"date": "%s", "description": "checkup", "prescription": "  "}""".formatted(today))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.prescription").value(nullValue()));
	}

	@Test
	void createVisitWithTooLongPrescription() throws Exception {
		String today = LocalDate.now().toString();
		postVisit("""
				{"date": "%s", "description": "checkup", "prescription": "%s"}""".formatted(today, "A".repeat(256)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'prescription')].code").value("Size"));
		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void createVisitWithMissingFields() throws Exception {
		postVisit("{}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'date')].code").value("NotNull"))
			.andExpect(jsonPath("$.errors[?(@.field == 'description')].code").value("NotBlank"));
		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void createVisitForAnEarlierDate() throws Exception {
		// paper records are entered later, so past dates are fine
		postVisit("""
				{"date": "2026-09-01", "description": "checkup"}""").andExpect(status().isCreated())
			.andExpect(jsonPath("$.date").value("2026-09-01"));
	}

	@Test
	void createVisitInTheFutureIsRejected() throws Exception {
		postVisit("""
				{"date": "%s", "description": "Visit Description"}""".formatted(LocalDate.now().plusDays(1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'date')].code").value("visit.dateInFuture"))
			.andExpect(
					jsonPath("$.errors[?(@.field == 'date')].message").value("The visit date cannot be in the future"));
		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void updateVisit() throws Exception {
		String today = LocalDate.now().toString();
		putVisit(TEST_VISIT_ID, """
				{"date": "%s", "description": "follow-up", "prescription": "Otomax"}""".formatted(today))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.prescription").value("Otomax"))
			.andExpect(jsonPath("$.id").value(TEST_VISIT_ID))
			.andExpect(jsonPath("$.date").value(today))
			.andExpect(jsonPath("$.description").value("follow-up"));
	}

	@Test
	void updateVisitInTheFutureIsRejected() throws Exception {
		putVisit(TEST_VISIT_ID, """
				{"date": "%s", "description": "follow-up"}""".formatted(LocalDate.now().plusDays(1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'date')].code").value("visit.dateInFuture"));
		assertThat(this.owner.getPet(TEST_PET_ID).getVisit(TEST_VISIT_ID).getDescription()).isEqualTo("rabies shot");
		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void updateUnknownVisit() throws Exception {
		putVisit(999, """
				{"date": "%s", "description": "follow-up"}""".formatted(LocalDate.now()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Visit 999 not found for pet 1"));
	}

	@Test
	void deleteVisit() throws Exception {
		mockMvc
			.perform(delete("/api/owners/{ownerId}/pets/{petId}/visits/{visitId}", TEST_OWNER_ID, TEST_PET_ID,
					TEST_VISIT_ID))
			.andExpect(status().isNoContent());
		assertThat(this.owner.getPet(TEST_PET_ID).getVisits()).isEmpty();
		verify(this.owners).save(this.owner);
	}

	@Test
	void deleteUnknownVisit() throws Exception {
		mockMvc.perform(delete("/api/owners/{ownerId}/pets/{petId}/visits/{visitId}", TEST_OWNER_ID, TEST_PET_ID, 999))
			.andExpect(status().isNotFound());
	}

}
