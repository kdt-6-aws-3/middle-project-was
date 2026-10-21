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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.entity.Appointment;
import org.springframework.samples.petclinic.entity.AppointmentStatus;
import org.springframework.samples.petclinic.entity.Hospital;
import org.springframework.samples.petclinic.entity.Owner;
import org.springframework.samples.petclinic.entity.Pet;
import org.springframework.samples.petclinic.entity.PetType;
import org.springframework.samples.petclinic.entity.Vet;
import org.springframework.samples.petclinic.entity.Visit;
import org.springframework.samples.petclinic.repository.AppointmentRepository;
import org.springframework.samples.petclinic.repository.HospitalRepository;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.repository.PetTypeRepository;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test of the repositories against the sample data in db/h2 (a copy of the
 * db/mysql schema and data).
 *
 * @author Ken Krebs
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Dave Syer
 */
@DataJpaTest
// Ensure that if the mysql profile is active we connect to the real database:
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ClinicServiceTests {

	@Autowired
	protected OwnerRepository owners;

	@Autowired
	protected PetTypeRepository types;

	@Autowired
	protected VetRepository vets;

	@Autowired
	protected HospitalRepository hospitals;

	@Autowired
	protected AppointmentRepository appointments;

	@Autowired
	private TestEntityManager entityManager;

	private final Pageable pageable = Pageable.unpaged();

	private PetType type(int id) {
		return EntityUtils.getById(this.types.findPetTypes(), PetType.class, id);
	}

	private Pet newPet(String name) {
		Pet pet = new Pet();
		pet.setName(name);
		pet.setType(type(1));
		pet.setBirthDate(LocalDate.now());
		return pet;
	}

	/**
	 * Writes pending changes to the database and forgets loaded entities, so the next
	 * lookup really reads the tables.
	 */
	private void flushAndClear() {
		this.entityManager.flush();
		this.entityManager.clear();
	}

	@Test
	void shouldFindOwnersByName() {
		Page<Owner> owners = this.owners.findByNameStartingWith("김", pageable);
		assertThat(owners).hasSize(1);
		assertThat(owners.getContent().get(0).getName()).isEqualTo("김지훈");

		owners = this.owners.findByNameStartingWith("김지훈X", pageable);
		assertThat(owners).isEmpty();
	}

	@Test
	void shouldFindSingleOwnerWithPets() {
		Optional<Owner> optionalOwner = this.owners.findById(1);
		assertThat(optionalOwner).isPresent();
		Owner owner = optionalOwner.get();
		assertThat(owner.getName()).isEqualTo("김지훈");
		assertThat(owner.getPhone()).isEqualTo("000-0000-2001");
		assertThat(owner.getPets()).extracting(Pet::getName).containsExactlyInAnyOrder("초코", "나비");
		assertThat(owner.getPet("초코").getType().getName()).isEqualTo("개");
	}

	@Test
	void shouldFindOwnerWithoutPets() {
		assertThat(this.owners.findById(10).orElseThrow().getPets()).isEmpty();
	}

	@Test
	@Transactional
	void shouldInsertOwner() {
		Page<Owner> owners = this.owners.findByNameStartingWith("테스트", pageable);
		int found = (int) owners.getTotalElements();

		Owner owner = new Owner();
		owner.setName("테스트보호자");
		owner.setAddress("서울특별시 테스트구");
		owner.setPhone("010-1234-5678");
		this.owners.save(owner);
		assertThat(owner.getId()).isNotNull();

		owners = this.owners.findByNameStartingWith("테스트", pageable);
		assertThat(owners.getTotalElements()).isEqualTo(found + 1);
	}

	@Test
	@Transactional
	void shouldUpdateOwner() {
		Owner owner = this.owners.findById(1).orElseThrow();
		String newName = owner.getName() + "X";

		owner.setName(newName);
		this.owners.save(owner);
		flushAndClear();

		assertThat(this.owners.findById(1).orElseThrow().getName()).isEqualTo(newName);
	}

	@Test
	@Transactional
	void shouldDeleteOwnerWithPetsAndVisits() {
		// owner 1 has two pets with visits and appointments: the foreign keys have no ON
		// DELETE CASCADE, so appointments are deleted first (as OwnerService does), then
		// JPA
		// deletes visits, pets and the owner
		Owner owner1 = this.owners.findById(1).orElseThrow();
		this.appointments.deleteByPetIds(owner1.getPets().stream().map(Pet::getId).toList());
		this.owners.delete(owner1);
		flushAndClear();

		assertThat(this.owners.findById(1)).isEmpty();
		assertThat(this.owners.count()).isEqualTo(9);
	}

	@Test
	void shouldFindAllPetTypes() {
		Collection<PetType> petTypes = this.types.findPetTypes();

		assertThat(petTypes).hasSize(6);
		assertThat(EntityUtils.getById(petTypes, PetType.class, 1).getName()).isEqualTo("개");
		assertThat(EntityUtils.getById(petTypes, PetType.class, 4).getName()).isEqualTo("햄스터");
	}

	@Test
	@Transactional
	void shouldInsertPetIntoDatabaseAndGenerateId() {
		Owner owner6 = this.owners.findById(6).orElseThrow();
		int found = owner6.getPets().size();

		owner6.addPet(newPet("bowser"));
		assertThat(owner6.getPets()).hasSize(found + 1);

		// pets.owner_id is NOT NULL: the owner id must be part of the INSERT
		this.owners.saveAndFlush(owner6);
		flushAndClear();

		owner6 = this.owners.findById(6).orElseThrow();
		assertThat(owner6.getPets()).hasSize(found + 1);
		// checks that id has been generated
		assertThat(owner6.getPet("bowser").getId()).isNotNull();
	}

	@Test
	@Transactional
	void shouldUpdatePetName() {
		Owner owner6 = this.owners.findById(6).orElseThrow();
		Pet pet7 = owner6.getPet(7);
		String newName = pet7.getName() + "X";

		pet7.setName(newName);
		this.owners.save(owner6);
		flushAndClear();

		assertThat(this.owners.findById(6).orElseThrow().getPet(7).getName()).isEqualTo(newName);
	}

	@Test
	@Transactional
	void shouldDeletePetWithVisits() {
		Owner owner1 = this.owners.findById(1).orElseThrow();
		this.appointments.deleteByPetIds(List.of(1));
		owner1.removePet(owner1.getPet(1));
		this.owners.save(owner1);
		flushAndClear();

		owner1 = this.owners.findById(1).orElseThrow();
		assertThat(owner1.getPet(1)).isNull();
		assertThat(owner1.getPets()).hasSize(1);
	}

	@Test
	void shouldFindVets() {
		Collection<Vet> vets = this.vets.findAll();
		assertThat(vets).hasSize(10);

		Vet vet = EntityUtils.getById(vets, Vet.class, 3);
		assertThat(vet.getName()).isEqualTo("박도윤");
		assertThat(vet.getHospital().getName()).isEqualTo("펫클리닉 송파점");
		assertThat(vet.getNrOfSpecialties()).isEqualTo(2);
		assertThat(vet.getSpecialties().get(0).getName()).isEqualTo("내과");
		assertThat(vet.getSpecialties().get(1).getName()).isEqualTo("영상의학과");

		assertThat(EntityUtils.getById(vets, Vet.class, 10).getSpecialties()).isEmpty();
	}

	@Test
	void shouldSearchAppointments() {
		assertThat(this.appointments.findAll()).hasSize(10);
		assertThat(this.appointments.search(null, null, null, AppointmentStatus.COMPLETED))
			.extracting(Appointment::getId)
			.containsExactly(1, 3, 2);
		assertThat(this.appointments.search(1, 1, 2, null)).extracting(Appointment::getReason).containsExactly("귀 가려움");
		assertThat(this.appointments.findById(10).orElseThrow().getReason()).isNull();
	}

	@Test
	@Transactional
	void shouldInsertAppointmentWithCreationTime() {
		Appointment appointment = new Appointment();
		appointment.setHospital(this.hospitals.findById(1).orElseThrow());
		appointment.setVet(this.vets.findById(1).orElseThrow());
		appointment.setPet(this.owners.findById(1).orElseThrow().getPet(1));
		appointment.setAppointmentAt(LocalDateTime.now().plusDays(1));
		appointment.setStatus(AppointmentStatus.REQUESTED);
		this.appointments.saveAndFlush(appointment);
		flushAndClear();

		Appointment reloaded = this.appointments.findById(appointment.getId()).orElseThrow();
		assertThat(reloaded.getCreatedAt()).isNotNull();
		assertThat(reloaded.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
		assertThat(reloaded.getVet().getName()).isEqualTo("김민준");
	}

	private Appointment newAppointment(int hospitalAndVetId, int petId, LocalDateTime at) {
		Appointment appointment = new Appointment();
		appointment.setHospital(this.hospitals.findById(hospitalAndVetId).orElseThrow());
		appointment.setVet(this.vets.findById(hospitalAndVetId).orElseThrow());
		appointment.setPet(EntityUtils
			.getById(this.owners.findAll().stream().flatMap(o -> o.getPets().stream()).toList(), Pet.class, petId));
		appointment.setAppointmentAt(at);
		appointment.setStatus(AppointmentStatus.REQUESTED);
		return appointment;
	}

	@Test
	void shouldTellWhetherASlotIsBooked() {
		LocalDateTime slot = LocalDateTime.of(2026, 10, 28, 10, 30);
		assertThat(this.appointments.isVetBooked(2, slot, null)).isTrue();
		assertThat(this.appointments.isVetBooked(2, slot, 4)).isFalse(); // its own slot
		assertThat(this.appointments.isPetBooked(3, slot, null)).isTrue();
		// appointment 7 (vet 5, pet 6) was cancelled, so its slot is free again
		assertThat(this.appointments.isVetBooked(5, LocalDateTime.of(2026, 10, 22, 13, 0), null)).isFalse();
	}

	@Test
	@Transactional
	void shouldRejectSecondBookingOfAVetSlotInTheDatabase() {
		// vet 2 is booked at 2026-10-28 10:30 (appointment 4); another pet must not get
		// it
		Appointment duplicate = newAppointment(2, 1, LocalDateTime.of(2026, 10, 28, 10, 30));
		assertThatThrownBy(() -> this.appointments.saveAndFlush(duplicate))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@Transactional
	void shouldAllowBookingACancelledSlot() {
		this.appointments.saveAndFlush(newAppointment(5, 6, LocalDateTime.of(2026, 10, 22, 13, 0)));
		assertThat(this.appointments.isVetBooked(5, LocalDateTime.of(2026, 10, 22, 13, 0), null)).isTrue();
	}

	@Test
	void shouldFindHospitalsAndTheirVets() {
		List<Hospital> hospitals = this.hospitals.findAllByOrderByIdAsc();
		assertThat(hospitals).hasSize(10);
		assertThat(hospitals.get(0).getName()).isEqualTo("펫클리닉 강남점");

		assertThat(this.vets.findByHospitalIdOrderByIdAsc(1)).extracting(Vet::getName).containsExactly("김민준");
	}

	@Test
	@Transactional
	void shouldAddNewVisitForPet() {
		Owner owner6 = this.owners.findById(6).orElseThrow();
		Pet pet7 = owner6.getPet(7);
		int found = pet7.getVisits().size();
		Visit visit = new Visit();
		visit.setDescription("test");

		owner6.addVisit(pet7.getId(), visit);
		// visits.pet_id is NOT NULL: the pet id must be part of the INSERT
		this.owners.saveAndFlush(owner6);
		flushAndClear();

		assertThat(this.owners.findById(6).orElseThrow().getPet(7).getVisits()) //
			.hasSize(found + 1) //
			.allMatch(value -> value.getId() != null);
	}

	@Test
	@Transactional
	void shouldDeleteVisit() {
		Owner owner1 = this.owners.findById(1).orElseThrow();
		Pet pet1 = owner1.getPet(1);
		int found = pet1.getVisits().size();
		pet1.removeVisit(pet1.getVisit(1));
		this.owners.save(owner1);
		flushAndClear();

		assertThat(this.owners.findById(1).orElseThrow().getPet(1).getVisits()).hasSize(found - 1);
	}

	@Test
	void shouldFindVisitsByPetId() {
		Pet pet1 = this.owners.findById(1).orElseThrow().getPet(1);

		assertThat(pet1.getVisits()) //
			.hasSize(2) //
			.element(0)
			.extracting(Visit::getDate)
			.isEqualTo(LocalDate.of(2026, 9, 1));
	}

	@Test
	@Transactional
	void shouldAllowDuplicatePetNameForSameOwner() {
		Owner owner = this.owners.findById(1).orElseThrow();
		int initialPetCount = owner.getPets().size();

		owner.addPet(newPet("DuplicatePetName"));
		this.owners.saveAndFlush(owner);
		owner.addPet(newPet("duplicatepetname")); // same name, different case
		this.owners.saveAndFlush(owner);
		flushAndClear();

		Owner reloaded = this.owners.findById(1).orElseThrow();
		assertThat(reloaded.getPets()).hasSize(initialPetCount + 2);
		assertThat(reloaded.getPets()).filteredOn(pet -> "duplicatepetname".equalsIgnoreCase(pet.getName())).hasSize(2);
	}

	@Test
	@Transactional
	void shouldAllowSamePetNameForDifferentOwners() {
		Owner owner1 = this.owners.findById(1).orElseThrow();
		owner1.addPet(newPet("SamePetName"));
		this.owners.saveAndFlush(owner1);

		Owner owner2 = this.owners.findById(2).orElseThrow();
		owner2.addPet(newPet("samepetname")); // same name, different owner
		this.owners.saveAndFlush(owner2);

		assertThat(owner1.getPet("SamePetName")).isNotNull();
		assertThat(owner2.getPet("samepetname")).isNotNull();
	}

}
