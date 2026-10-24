package org.springframework.samples.petclinic.entity;

public enum AppointmentStatus {

	REQUESTED, CONFIRMED, CANCELLED, COMPLETED;

	public boolean isClosed() {
		return this == CANCELLED || this == COMPLETED;
	}

}
