package org.springframework.samples.petclinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.samples.petclinic.entity.Hospital;

public interface HospitalRepository extends JpaRepository<Hospital, Integer> {

	List<Hospital> findAllByOrderByIdAsc();

}
