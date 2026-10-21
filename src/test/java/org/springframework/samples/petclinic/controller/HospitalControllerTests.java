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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.entity.Hospital;
import org.springframework.samples.petclinic.entity.Vet;
import org.springframework.samples.petclinic.repository.HospitalRepository;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.samples.petclinic.service.HospitalService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test class for the {@link HospitalController}
 */
@WebMvcTest(HospitalController.class)
@Import(HospitalService.class)
@DisabledInNativeImage
@DisabledInAotMode
class HospitalControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HospitalRepository hospitals;

	@MockitoBean
	private VetRepository vets;

	@BeforeEach
	void setup() {
		Hospital gangnam = VetControllerTests.gangnam();
		Hospital seocho = new Hospital();
		seocho.setId(2);
		seocho.setName("Seocho");
		seocho.setAddress("Seoul Seocho-gu");
		seocho.setPhone("000-0000-1002");
		Vet james = new Vet();
		james.setId(1);
		james.setName("James Carter");
		james.setHospital(gangnam);
		given(this.hospitals.findAllByOrderByIdAsc()).willReturn(List.of(gangnam, seocho));
		given(this.hospitals.findById(1)).willReturn(Optional.of(gangnam));
		given(this.vets.findByHospitalIdOrderByIdAsc(1)).willReturn(List.of(james));
	}

	@Test
	void listHospitals() throws Exception {
		mockMvc.perform(get("/api/hospitals"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].name").value("Gangnam"))
			.andExpect(jsonPath("$[0].address").value("Seoul Gangnam-gu"))
			.andExpect(jsonPath("$[1].phone").value("000-0000-1002"));
	}

	@Test
	void showHospital() throws Exception {
		mockMvc.perform(get("/api/hospitals/{hospitalId}", 1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Gangnam"));
	}

	@Test
	void showHospitalNotFound() throws Exception {
		mockMvc.perform(get("/api/hospitals/{hospitalId}", 99))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Hospital 99 not found"));
	}

	@Test
	void listVetsOfHospital() throws Exception {
		mockMvc.perform(get("/api/hospitals/{hospitalId}/vets", 1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].name").value("James Carter"))
			.andExpect(jsonPath("$[0].hospital.id").value(1));
	}

	@Test
	void listVetsOfUnknownHospital() throws Exception {
		mockMvc.perform(get("/api/hospitals/{hospitalId}/vets", 99)).andExpect(status().isNotFound());
	}

}
