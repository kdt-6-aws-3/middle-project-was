package org.springframework.samples.petclinic.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.samples.petclinic.dto.AppointmentDetails;
import org.springframework.samples.petclinic.entity.Appointment;
import org.springframework.samples.petclinic.entity.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

	@Query(value = """
			SELECT new org.springframework.samples.petclinic.dto.AppointmentDetails(
			    a.id, h.id, h.name, p.id, p.name, v.id, v.name, a.appointmentAt, a.status, a.reason, a.createdAt)
			FROM Appointment a JOIN a.hospital h JOIN a.pet p JOIN a.vet v
			WHERE (:hospitalId IS NULL OR a.hospital.id = :hospitalId)
			  AND (:vetId IS NULL OR a.vet.id = :vetId)
			  AND (:petId IS NULL OR a.pet.id = :petId)
			  AND (:status IS NULL OR a.status = :status)
			ORDER BY a.appointmentAt, a.id""", countQuery = """
			SELECT COUNT(a) FROM Appointment a
			WHERE (:hospitalId IS NULL OR a.hospital.id = :hospitalId)
			  AND (:vetId IS NULL OR a.vet.id = :vetId)
			  AND (:petId IS NULL OR a.pet.id = :petId)
			  AND (:status IS NULL OR a.status = :status)""")
	Page<AppointmentDetails> search(@Param("hospitalId") Integer hospitalId, @Param("vetId") Integer vetId,
			@Param("petId") Integer petId, @Param("status") AppointmentStatus status, Pageable pageable);

	@Query("""
			SELECT new org.springframework.samples.petclinic.dto.AppointmentDetails(
			    a.id, h.id, h.name, p.id, p.name, v.id, v.name, a.appointmentAt, a.status, a.reason, a.createdAt)
			FROM Appointment a JOIN a.hospital h JOIN a.pet p JOIN a.vet v
			WHERE a.vet.id = :vetId AND a.appointmentAt >= :from AND a.appointmentAt < :to
			ORDER BY a.appointmentAt, a.id""")
	List<AppointmentDetails> findVetSchedule(@Param("vetId") Integer vetId, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	@Query("""
			SELECT COUNT(a) > 0 FROM Appointment a
			WHERE a.vet.id = :vetId AND a.appointmentAt = :appointmentAt
			  AND a.status <> org.springframework.samples.petclinic.entity.AppointmentStatus.CANCELLED
			  AND (:excludeId IS NULL OR a.id <> :excludeId)""")
	boolean isVetBooked(@Param("vetId") Integer vetId, @Param("appointmentAt") LocalDateTime appointmentAt,
			@Param("excludeId") Integer excludeId);

	@Query("""
			SELECT COUNT(a) > 0 FROM Appointment a
			WHERE a.pet.id = :petId AND a.appointmentAt = :appointmentAt
			  AND a.status <> org.springframework.samples.petclinic.entity.AppointmentStatus.CANCELLED
			  AND (:excludeId IS NULL OR a.id <> :excludeId)""")
	boolean isPetBooked(@Param("petId") Integer petId, @Param("appointmentAt") LocalDateTime appointmentAt,
			@Param("excludeId") Integer excludeId);

	@Modifying
	@Query("DELETE FROM Appointment a WHERE a.pet.id IN :petIds")
	void deleteByPetIds(@Param("petIds") Collection<Integer> petIds);

	@Modifying
	@Query("DELETE FROM Appointment a")
	int deleteAllAppointments();

}
