package org.springframework.samples.petclinic.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.samples.petclinic.entity.Owner;

public interface OwnerRepository extends JpaRepository<Owner, Integer> {

	Page<Owner> findByNameStartingWith(String name, Pageable pageable);

}
