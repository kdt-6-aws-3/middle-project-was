package org.springframework.samples.petclinic.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "visits")
public class Visit extends BaseEntity {

	@Column(name = "visit_date")
	private LocalDate date;

	@NotBlank
	@Size(max = 255)
	private String description;

	@Size(max = 255)
	private String prescription;

	public Visit() {
		this.date = LocalDate.now();
	}

	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPrescription() {
		return this.prescription;
	}

	public void setPrescription(String prescription) {
		this.prescription = prescription;
	}

}
