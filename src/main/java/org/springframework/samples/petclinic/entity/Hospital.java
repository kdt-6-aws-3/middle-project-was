package org.springframework.samples.petclinic.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "hospitals")
@AttributeOverride(name = "name", column = @Column(name = "hospital_name"))
public class Hospital extends NamedEntity {

	@Column
	@NotBlank
	@Size(max = 255)
	private String address;

	@Column
	@NotBlank
	@Size(max = 50)
	private String phone;

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPhone() {
		return this.phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

}
