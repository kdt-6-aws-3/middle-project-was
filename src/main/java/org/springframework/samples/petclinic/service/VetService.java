package org.springframework.samples.petclinic.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.dto.PageResponse;
import org.springframework.samples.petclinic.dto.VetDetails;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.stereotype.Service;

@Service
public class VetService {

	private static final int MAX_PAGE_SIZE = 100;

	private final VetRepository vetRepository;

	public VetService(VetRepository vetRepository) {
		this.vetRepository = vetRepository;
	}

	public PageResponse<VetDetails> listVets(int page, int size) {
		int validatedPage = Math.max(page, 1);
		int validatedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
		return PageResponse.from(
				this.vetRepository.findAll(PageRequest.of(validatedPage - 1, validatedSize, Sort.by("id"))),
				VetDetails::from);
	}

}
