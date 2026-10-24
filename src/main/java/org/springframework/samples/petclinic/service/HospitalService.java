package org.springframework.samples.petclinic.service;

import java.util.List;

import org.springframework.samples.petclinic.dto.HospitalDetails;
import org.springframework.samples.petclinic.dto.VetDetails;
import org.springframework.samples.petclinic.entity.Hospital;
import org.springframework.samples.petclinic.exception.ResourceNotFoundException;
import org.springframework.samples.petclinic.repository.HospitalRepository;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.stereotype.Service;

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
