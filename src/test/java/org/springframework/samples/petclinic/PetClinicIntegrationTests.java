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

package org.springframework.samples.petclinic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "logging.level.sql=DEBUG")
public class PetClinicIntegrationTests {

	private static final Pattern FIRST_ID = Pattern.compile("\"id\":(\\d+)");

	@LocalServerPort
	int port;

	@Autowired
	private VetRepository vets;

	@Autowired
	private RestTemplateBuilder builder;

	private RestTemplate template() {
		return this.builder.baseUri("http://localhost:" + this.port).build();
	}

	private static int idOf(ResponseEntity<String> response) {
		Matcher matcher = FIRST_ID.matcher(response.getBody());
		assertThat(matcher.find()).isTrue();
		return Integer.parseInt(matcher.group(1));
	}

	private ResponseEntity<String> send(RequestEntity<?> request) {
		return template().exchange(request, String.class);
	}

	@Test
	void findAll() {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void ownerDetails() {
		ResponseEntity<String> result = send(RequestEntity.get("/api/owners/1").build());
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(result.getBody()).contains("\"name\":\"김지훈\"").contains("\"phone\":\"000-0000-2001\"");
	}

	@Test
	void ownerList() {
		ResponseEntity<String> result = send(RequestEntity.get("/api/owners?name=").build());
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("\"totalElements\":10");
	}

	@Test
	void vetsIncludeTheirHospital() {
		ResponseEntity<String> result = send(RequestEntity.get("/api/vets").build());
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("\"hospital\":{\"id\":1,\"name\":\"펫클리닉 강남점\"");
	}

	@Test
	void hospitalsAndTheirVets() {
		assertThat(send(RequestEntity.get("/api/hospitals").build()).getBody()).contains("펫클리닉 부산점");
		assertThat(send(RequestEntity.get("/api/hospitals/1/vets").build()).getBody()).contains("김민준");
	}

	/**
	 * Full create / update / delete cycle against the real schema: the NOT NULL foreign
	 * keys (pets.owner_id, visits.pet_id) and the missing ON DELETE CASCADE are both
	 * exercised here.
	 */
	@Test
	void crudCycleOfOwnerPetAndVisit() {
		String tomorrow = LocalDate.now().plusDays(1).toString();

		ResponseEntity<String> owner = send(RequestEntity.post("/api/owners")
			.contentType(MediaType.APPLICATION_JSON)
			.body("{\"name\":\"CRUD Owner\",\"address\":\"Seoul\",\"phone\":\"010-1234-5678\"}"));
		assertThat(owner.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		int ownerId = idOf(owner);

		ResponseEntity<String> pet = send(RequestEntity.post("/api/owners/" + ownerId + "/pets")
			.contentType(MediaType.APPLICATION_JSON)
			.body("{\"name\":\"Coco\",\"birthDate\":\"2020-01-01\",\"typeId\":1}"));
		assertThat(pet.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		int petId = idOf(pet);
		String visits = "/api/owners/" + ownerId + "/pets/" + petId + "/visits";

		ResponseEntity<String> visit = send(RequestEntity.post(visits)
			.contentType(MediaType.APPLICATION_JSON)
			.body("{\"date\":\"" + tomorrow + "\",\"description\":\"checkup\"}"));
		assertThat(visit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		int visitId = idOf(visit);

		ResponseEntity<String> updated = send(RequestEntity.put(visits + "/" + visitId)
			.contentType(MediaType.APPLICATION_JSON)
			.body("{\"date\":\"" + tomorrow + "\",\"description\":\"follow-up\"}"));
		assertThat(updated.getBody()).contains("follow-up");

		assertThat(send(RequestEntity.delete(visits + "/" + visitId).build()).getStatusCode())
			.isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(send(RequestEntity.get(visits).build()).getBody()).isEqualTo("[]");

		// a second visit stays, so deleting the owner must also delete pet and visit
		send(RequestEntity.post(visits)
			.contentType(MediaType.APPLICATION_JSON)
			.body("{\"date\":\"" + tomorrow + "\",\"description\":\"again\"}"));
		assertThat(send(RequestEntity.delete("/api/owners/" + ownerId).build()).getStatusCode())
			.isEqualTo(HttpStatus.NO_CONTENT);

		HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
				() -> send(RequestEntity.get("/api/owners/" + ownerId).build()));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void unknownOwnerIsProblemDetail() {
		HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
				() -> send(RequestEntity.get("/api/owners/99999").build()));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(ex.getResponseHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(ex.getResponseBodyAsString()).contains("Owner 99999 not found");
	}

	@Test
	void pagesAreNotServed() {
		HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
				() -> send(RequestEntity.get("/").build()));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void healthIsUpAndOtherActuatorEndpointsAreHidden() {
		ResponseEntity<String> health = send(RequestEntity.get("/actuator/health").build());
		assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(health.getBody()).contains("\"status\":\"UP\"");
		HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
				() -> send(RequestEntity.get("/actuator/env").build()));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	public static void main(String[] args) {
		SpringApplication.run(PetClinicApplication.class, "--spring.docker.compose.lifecycle-management=NONE");
	}

}
