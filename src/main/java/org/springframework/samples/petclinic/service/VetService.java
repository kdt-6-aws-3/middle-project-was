package org.springframework.samples.petclinic.service;

import java.util.List;

import org.springframework.samples.petclinic.dto.VetDetails;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.stereotype.Service;

@Service
public class VetService {

	private final VetRepository vetRepository;

	public VetService(VetRepository vetRepository) {
		this.vetRepository = vetRepository;
	}

	public List<VetDetails> listVets() {
		return this.vetRepository.findAll().stream().map(VetDetails::from).toList();
	}

}
