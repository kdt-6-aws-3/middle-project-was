package org.springframework.samples.petclinic.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {

	@ManyToOne(optional = false)
	@JoinColumn(name = "hospital_id")
	private Hospital hospital;

	@ManyToOne(optional = false)
	@JoinColumn(name = "pet_id")
	private Pet pet;

	@ManyToOne(optional = false)
	@JoinColumn(name = "vet_id")
	private Vet vet;

	@Column(name = "appointment_at")
	private LocalDateTime appointmentAt;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private AppointmentStatus status;

	@Column
	private String reason;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
	}

	public Hospital getHospital() {
		return this.hospital;
	}

	public void setHospital(Hospital hospital) {
		this.hospital = hospital;
	}

	public Pet getPet() {
		return this.pet;
	}

	public void setPet(Pet pet) {
		this.pet = pet;
	}

	public Vet getVet() {
		return this.vet;
	}

	public void setVet(Vet vet) {
		this.vet = vet;
	}

	public LocalDateTime getAppointmentAt() {
		return this.appointmentAt;
	}

	public void setAppointmentAt(LocalDateTime appointmentAt) {
		this.appointmentAt = appointmentAt;
	}

	public AppointmentStatus getStatus() {
		return this.status;
	}

	public void setStatus(AppointmentStatus status) {
		this.status = status;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

}
