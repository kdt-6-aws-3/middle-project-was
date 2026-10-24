package org.springframework.samples.petclinic.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;
import org.springframework.samples.petclinic.entity.Pet;

public interface PetRepository extends Repository<Pet, Integer> {

	Optional<Pet> findById(Integer id);

}
