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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * Test class for the {@link AppointmentController}
 */
@WebMvcTest(AppointmentController.class)
@Import(AppointmentService.class)
@DisabledInNativeImage
@DisabledInAotMode
class AppointmentControllerTests {

	private static final String TOMORROW = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.HOURS).toString();

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

	private Appointment booked;

	private Appointment completed;

	private static Hospital hospital(int id, String name) {
		Hospital hospital = new Hospital();
		hospital.setId(id);
		hospital.setName(name);
		return hospital;
	}

	private static Vet vet(int id, String name, Hospital hospital) {
		Vet vet = new Vet();
		vet.setId(id);
		vet.setName(name);
		vet.setHospital(hospital);
		return vet;
	}

	private static Appointment appointment(int id, Hospital hospital, Pet pet, Vet vet, AppointmentStatus status) {
		Appointment appointment = new Appointment();
		appointment.setId(id);
		appointment.setHospital(hospital);
		appointment.setPet(pet);
		appointment.setVet(vet);
		appointment.setAppointmentAt(LocalDateTime.of(2026, 10, 28, 10, 30));
		appointment.setStatus(status);
		appointment.setReason("checkup");
		appointment.setCreatedAt(LocalDateTime.of(2026, 10, 15, 9, 0));
		return appointment;
	}

	@BeforeEach
	void setup() {
		Hospital gangnam = hospital(1, "Gangnam");
		Hospital seocho = hospital(2, "Seocho");
		Vet kim = vet(1, "Kim", gangnam);
		Vet lee = vet(2, "Lee", seocho);
		Pet choco = new Pet();
		choco.setId(1);
		choco.setName("Choco");
		given(this.hospitals.findById(1)).willReturn(Optional.of(gangnam));
		given(this.hospitals.findById(2)).willReturn(Optional.of(seocho));
		given(this.vets.findById(1)).willReturn(Optional.of(kim));
		given(this.vets.findById(2)).willReturn(Optional.of(lee));
		given(this.pets.findById(1)).willReturn(Optional.of(choco));

		this.booked = appointment(4, gangnam, choco, kim, AppointmentStatus.CONFIRMED);
		this.completed = appointment(1, gangnam, choco, kim, AppointmentStatus.COMPLETED);
		given(this.appointments.findById(4)).willReturn(Optional.of(this.booked));
		given(this.appointments.findById(1)).willReturn(Optional.of(this.completed));
		given(this.appointments.search(isNull(), isNull(), isNull(), isNull()))
			.willReturn(List.of(this.completed, this.booked));
		given(this.appointments.search(isNull(), isNull(), isNull(), any(AppointmentStatus.class)))
			.willReturn(List.of(this.booked));
		given(this.appointments.save(any(Appointment.class))).willAnswer(invocation -> {
			Appointment saved = invocation.getArgument(0);
			if (saved.isNew()) {
				saved.setId(11);
				saved.setCreatedAt(LocalDateTime.of(2026, 10, 21, 12, 0));
			}
			return saved;
		});
	}

	private ResultActions postAppointment(String json) throws Exception {
		return mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String request(int hospitalId, int petId, int vetId, String at) {
		return """
				{"hospitalId": %d, "petId": %d, "vetId": %d, "appointmentAt": "%s", "reason": "vaccination"}"""
			.formatted(hospitalId, petId, vetId, at);
	}

	@Test
	void listAppointments() throws Exception {
		mockMvc.perform(get("/api/appointments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[1].id").value(4))
			.andExpect(jsonPath("$[1].hospitalName").value("Gangnam"))
			.andExpect(jsonPath("$[1].petName").value("Choco"))
			.andExpect(jsonPath("$[1].vetName").value("Kim"))
			.andExpect(jsonPath("$[1].appointmentAt").value("2026-10-28T10:30:00"))
			.andExpect(jsonPath("$[1].status").value("CONFIRMED"));
	}

	@Test
	void listAppointmentsByStatus() throws Exception {
		mockMvc.perform(get("/api/appointments").param("status", "CONFIRMED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));
		verify(this.appointments).search(null, null, null, AppointmentStatus.CONFIRMED);
	}

	@Test
	void listAppointmentsWithUnknownStatus() throws Exception {
		mockMvc.perform(get("/api/appointments").param("status", "LOST")).andExpect(status().isBadRequest());
	}

	@Test
	void showAppointment() throws Exception {
		mockMvc.perform(get("/api/appointments/{id}", 4))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reason").value("checkup"))
			.andExpect(jsonPath("$.createdAt").value("2026-10-15T09:00:00"));
	}

	@Test
	void showAppointmentNotFound() throws Exception {
		mockMvc.perform(get("/api/appointments/{id}", 99))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Appointment 99 not found"));
	}

	@Test
	void createAppointment() throws Exception {
		postAppointment(request(1, 1, 1, TOMORROW)).andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/appointments/11"))
			.andExpect(jsonPath("$.id").value(11))
			.andExpect(jsonPath("$.status").value("REQUESTED"))
			.andExpect(jsonPath("$.reason").value("vaccination"))
			.andExpect(jsonPath("$.createdAt").value("2026-10-21T12:00:00"));
	}

	@Test
	void createAppointmentWithMissingFields() throws Exception {
		postAppointment("{}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors", hasSize(4)))
			.andExpect(jsonPath("$.errors[?(@.field == 'appointmentAt')].code").value("NotNull"));
		verify(this.appointments, never()).save(any(Appointment.class));
	}

	@Test
	void createAppointmentWithVetOfAnotherHospital() throws Exception {
		postAppointment(request(1, 1, 2, TOMORROW)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'vetId')].code").value("appointment.vetNotInHospital"))
			.andExpect(jsonPath("$.errors[?(@.field == 'vetId')].message")
				.value("The vet does not work at this hospital"));
		verify(this.appointments, never()).save(any(Appointment.class));
	}

	@Test
	void createAppointmentInThePast() throws Exception {
		postAppointment(request(1, 1, 1, "2020-01-01T10:00:00")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'appointmentAt')].code").value("appointment.notInFuture"));
	}

	@Test
	void createAppointmentWithUnknownPet() throws Exception {
		postAppointment(request(1, 99, 1, TOMORROW)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'petId')].code").value("notFound"));
	}

	@Test
	void updateAppointment() throws Exception {
		mockMvc
			.perform(put("/api/appointments/{id}", 4).contentType(MediaType.APPLICATION_JSON)
				.content(request(1, 1, 1, TOMORROW)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(4))
			.andExpect(jsonPath("$.appointmentAt").value(TOMORROW + ":00"))
			.andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	@Test
	void updateCompletedAppointmentIsRejected() throws Exception {
		mockMvc
			.perform(put("/api/appointments/{id}", 1).contentType(MediaType.APPLICATION_JSON)
				.content(request(1, 1, 1, TOMORROW)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'status')].code").value("appointment.closed"));
		verify(this.appointments, never()).save(any(Appointment.class));
	}

	@Test
	void changeStatus() throws Exception {
		mockMvc
			.perform(patch("/api/appointments/{id}/status", 4).contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"CANCELLED\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
		assertThat(this.booked.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
	}

	@Test
	void changeStatusOfCompletedAppointmentIsRejected() throws Exception {
		mockMvc
			.perform(patch("/api/appointments/{id}/status", 1).contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"REQUESTED\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'status')].message")
				.value("A cancelled or completed appointment cannot be changed"));
	}

	@Test
	void changeStatusToUnknownValue() throws Exception {
		mockMvc
			.perform(patch("/api/appointments/{id}/status", 4).contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"LOST\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void deleteAppointment() throws Exception {
		mockMvc.perform(delete("/api/appointments/{id}", 4)).andExpect(status().isNoContent());
		verify(this.appointments).delete(this.booked);
	}

	@Test
	void deleteAppointmentNotFound() throws Exception {
		mockMvc.perform(delete("/api/appointments/{id}", 99)).andExpect(status().isNotFound());
	}

}
