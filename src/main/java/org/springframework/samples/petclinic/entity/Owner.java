package org.springframework.samples.petclinic.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.util.Assert;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "owners")
@AttributeOverride(name = "name", column = @Column(name = "owner_name"))
public class Owner extends NamedEntity {

	public static final String PHONE_PATTERN = "\\d{2,3}-\\d{3,4}-\\d{4}";

	@Column
	@NotBlank
	@Size(max = 255)
	private String address;

	@Column
	@NotBlank
	@Pattern(regexp = PHONE_PATTERN, message = "{phone.invalid}")
	private String phone;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "owner_id", nullable = false)
	@OrderBy("name")
	private final List<Pet> pets = new ArrayList<>();

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

	public List<Pet> getPets() {
		return this.pets;
	}

	public void addPet(Pet pet) {
		if (pet == null) {
			return;
		}
		if (getPets().contains(pet)) {
			return;
		}
		for (Pet existingPet : getPets()) {
			if (!existingPet.isNew() && !pet.isNew() && Objects.equals(existingPet.getId(), pet.getId())) {
				return;
			}
		}
		getPets().add(pet);
	}

	public void removePet(Pet pet) {
		getPets().remove(pet);
	}

	public Pet getPet(Integer id) {
		for (Pet pet : getPets()) {
			if (!pet.isNew()) {
				Integer compId = pet.getId();
				if (Objects.equals(compId, id)) {
					return pet;
				}
			}
		}
		return null;
	}

	public void addVisit(Integer petId, Visit visit) {

		Assert.notNull(petId, "Pet identifier must not be null!");
		Assert.notNull(visit, "Visit must not be null!");

		Pet pet = getPet(petId);

		Assert.notNull(pet, "Invalid Pet identifier!");

		pet.addVisit(visit);
	}

}
