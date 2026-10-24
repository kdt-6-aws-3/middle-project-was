package org.springframework.samples.petclinic.dto;

import java.time.LocalDate;

import org.springframework.samples.petclinic.entity.Visit;

public record VisitDetails(Integer id, LocalDate date, String description, String prescription) {

	public static VisitDetails from(Visit visit) {
		return new VisitDetails(visit.getId(), visit.getDate(), visit.getDescription(), visit.getPrescription());
	}

}
