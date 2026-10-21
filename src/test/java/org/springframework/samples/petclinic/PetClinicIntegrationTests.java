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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.entity.Owner;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "logging.level.sql=DEBUG")
public class PetClinicIntegrationTests {

	@LocalServerPort
	int port;

	@Autowired
	private VetRepository vets;

	@Autowired
	private RestTemplateBuilder builder;

	@Test
	void findAll() {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void ownerDetails() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template.exchange(RequestEntity.get("/api/owners/1").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(result.getBody()).contains("\"lastName\":\"Franklin\"");
	}

	@Test
	void ownerList() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template.exchange(RequestEntity.get("/api/owners?lastName=").build(),
				String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("\"totalElements\":10");
	}

	@Test
	void unknownOwnerIsProblemDetail() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
				() -> template.exchange(RequestEntity.get("/api/owners/99999").build(), String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(ex.getResponseHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(ex.getResponseBodyAsString()).contains("Owner 99999 not found");
	}

	@Test
	void pagesAreNotServed() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
				() -> template.exchange(RequestEntity.get("/").build(), String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void healthIsUpAndOtherActuatorEndpointsAreHidden() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<String> health = template.exchange(RequestEntity.get("/actuator/health").build(), String.class);
		assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(health.getBody()).contains("\"status\":\"UP\"");
		HttpClientErrorException ex = catchThrowableOfType(HttpClientErrorException.class,
				() -> template.exchange(RequestEntity.get("/actuator/env").build(), String.class));
		assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	public static void main(String[] args) {
		SpringApplication.run(PetClinicApplication.class, "--spring.docker.compose.lifecycle-management=NONE");
	}

}
