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

package org.springframework.samples.petclinic.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.samples.petclinic.entity.Appointment;
import org.springframework.samples.petclinic.entity.AppointmentStatus;

/**
 * Repository class for <code>Appointment</code> domain objects.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

	/**
	 * Appointments matching every given filter (a {@code null} filter matches all),
	 * earliest first.
	 */
	@Query("""
			SELECT a FROM Appointment a
			WHERE (:hospitalId IS NULL OR a.hospital.id = :hospitalId)
			  AND (:vetId IS NULL OR a.vet.id = :vetId)
			  AND (:petId IS NULL OR a.pet.id = :petId)
			  AND (:status IS NULL OR a.status = :status)
			ORDER BY a.appointmentAt, a.id""")
	List<Appointment> search(@Param("hospitalId") Integer hospitalId, @Param("vetId") Integer vetId,
			@Param("petId") Integer petId, @Param("status") AppointmentStatus status);

	/**
	 * Deletes the appointments of the given pets. Needed before deleting pets because
	 * appointments.pet_id has no ON DELETE CASCADE.
	 */
	@Modifying
	@Query("DELETE FROM Appointment a WHERE a.pet.id IN :petIds")
	void deleteByPetIds(@Param("petIds") Collection<Integer> petIds);

}
