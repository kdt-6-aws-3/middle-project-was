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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.entity.Appointment;
import org.springframework.samples.petclinic.entity.AppointmentStatus;
import org.springframework.samples.petclinic.entity.Hospital;
import org.springframework.samples.petclinic.entity.Pet;
import org.springframework.samples.petclinic.entity.Vet;
import org.springframework.samples.petclinic.repository.AppointmentRepository;
import org.springframework.samples.petclinic.repository.HospitalRepository;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.samples.petclinic.service.AppointmentService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test class for the {@link VetAppointmentController}
 */
@WebMvcTest(VetAppointmentController.class)
@Import(AppointmentService.class)
@DisabledInNativeImage
@DisabledInAotMode
class VetAppointmentControllerTests {

	private static final LocalDateTime DAY_START = LocalDateTime.of(2026, 10, 23, 0, 0);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AppointmentRepository appointments;

	@MockitoBean
	private HospitalRepository hospitals;

	@MockitoBean
	private PetRepository pets;

	@MockitoBean
	private VetRepository vets;

	@BeforeEach
	void setup() {
		Hospital gangnam = VetControllerTests.gangnam();
		Vet kim = new Vet();
		kim.setId(1);
		kim.setName("Kim");
		kim.setHospital(gangnam);
		Pet choco = new Pet();
		choco.setId(1);
		choco.setName("Choco");
		Appointment morning = new Appointment();
		morning.setId(11);
		morning.setHospital(gangnam);
		morning.setPet(choco);
		morning.setVet(kim);
		morning.setAppointmentAt(LocalDateTime.of(2026, 10, 23, 9, 0));
		morning.setStatus(AppointmentStatus.CONFIRMED);
		given(this.vets.findById(1)).willReturn(Optional.of(kim));
		given(this.appointments.findVetSchedule(1, DAY_START, DAY_START.plusDays(1))).willReturn(List.of(morning));
	}

	@Test
	void vetScheduleOfADay() throws Exception {
		mockMvc.perform(get("/api/vets/{vetId}/appointments", 1).param("date", "2026-10-23"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(11))
			.andExpect(jsonPath("$[0].appointmentAt").value("2026-10-23T09:00:00"))
			.andExpect(jsonPath("$[0].vetName").value("Kim"))
			.andExpect(jsonPath("$[0].status").value("CONFIRMED"));
		// the day is 00:00 (inclusive) to the next 00:00 (exclusive)
		verify(this.appointments).findVetSchedule(1, DAY_START, DAY_START.plusDays(1));
	}

	@Test
	void vetScheduleOfAFreeDayIsEmpty() throws Exception {
		mockMvc.perform(get("/api/vets/{vetId}/appointments", 1).param("date", "2026-10-24"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void vetScheduleNeedsADate() throws Exception {
		mockMvc.perform(get("/api/vets/{vetId}/appointments", 1)).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/vets/{vetId}/appointments", 1).param("date", "2026/10/23"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void vetScheduleOfUnknownVet() throws Exception {
		mockMvc.perform(get("/api/vets/{vetId}/appointments", 99).param("date", "2026-10-23"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Vet 99 not found"));
	}

}
