package org.springframework.samples.petclinic.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;
import org.springframework.samples.petclinic.entity.Vet;
import org.springframework.transaction.annotation.Transactional;

public interface VetRepository extends Repository<Vet, Integer> {

	@Transactional(readOnly = true)
	Collection<Vet> findAll();

	@Transactional(readOnly = true)
	Optional<Vet> findById(Integer id);

	@Transactional(readOnly = true)
	List<Vet> findByHospitalIdOrderByIdAsc(Integer hospitalId);

}
