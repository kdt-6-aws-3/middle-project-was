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

package org.springframework.samples.petclinic.service;

import java.util.List;

import org.springframework.samples.petclinic.dto.HospitalDetails;
import org.springframework.samples.petclinic.dto.VetDetails;
import org.springframework.samples.petclinic.entity.Hospital;
import org.springframework.samples.petclinic.exception.ResourceNotFoundException;
import org.springframework.samples.petclinic.repository.HospitalRepository;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.stereotype.Service;

/**
 * Hospitals and the veterinarians working at each of them (read only).
 */
@Service
public class HospitalService {

	private final HospitalRepository hospitals;

	private final VetRepository vets;

	public HospitalService(HospitalRepository hospitals, VetRepository vets) {
		this.hospitals = hospitals;
		this.vets = vets;
	}

	public List<HospitalDetails> listHospitals() {
		return this.hospitals.findAllByOrderByIdAsc().stream().map(HospitalDetails::from).toList();
	}

	public HospitalDetails getHospital(int hospitalId) {
		return HospitalDetails.from(findHospital(hospitalId));
	}

	public List<VetDetails> listVets(int hospitalId) {
		findHospital(hospitalId);
		return this.vets.findByHospitalIdOrderByIdAsc(hospitalId).stream().map(VetDetails::from).toList();
	}

	private Hospital findHospital(int hospitalId) {
		return this.hospitals.findById(hospitalId)
			.orElseThrow(() -> new ResourceNotFoundException("Hospital " + hospitalId + " not found"));
	}

}
