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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.entity.Hospital;
import org.springframework.samples.petclinic.entity.Specialty;
import org.springframework.samples.petclinic.entity.Vet;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.samples.petclinic.service.VetService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test class for the {@link VetController}
 */
@WebMvcTest(VetController.class)
@Import(VetService.class)
@DisabledInNativeImage
@DisabledInAotMode
class VetControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VetRepository vets;

	static Hospital gangnam() {
		Hospital gangnam = new Hospital();
		gangnam.setId(1);
		gangnam.setName("Gangnam");
		gangnam.setAddress("Seoul Gangnam-gu");
		gangnam.setPhone("000-0000-1001");
		return gangnam;
	}

	private Vet james() {
		Vet james = new Vet();
		james.setName("James Carter");
		james.setId(1);
		james.setHospital(gangnam());
		return james;
	}

	private Vet helen() {
		Vet helen = new Vet();
		helen.setName("Helen Leary");
		helen.setId(2);
		helen.setHospital(gangnam());
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		helen.addSpecialty(radiology);
		return helen;
	}

	@BeforeEach
	void setup() {
		given(this.vets.findAll()).willReturn(List.of(james(), helen()));
	}

	@Test
	void listVets() throws Exception {
		mockMvc.perform(get("/api/vets").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].id").value(1))
			.andExpect(jsonPath("$[0].name").value("James Carter"))
			.andExpect(jsonPath("$[0].hospital.id").value(1))
			.andExpect(jsonPath("$[0].hospital.name").value("Gangnam"))
			.andExpect(jsonPath("$[0].hospital.phone").value("000-0000-1001"))
			.andExpect(jsonPath("$[0].specialties", hasSize(0)))
			.andExpect(jsonPath("$[1].name").value("Helen Leary"))
			.andExpect(jsonPath("$[1].specialties[0].name").value("radiology"));
	}

}
