package org.springframework.samples.petclinic.entity;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "pets")
@AttributeOverride(name = "name", column = @Column(name = "pet_name"))
public class Pet extends NamedEntity {

	@Column
	private LocalDate birthDate;

	@ManyToOne
	@JoinColumn(name = "type_id")
	private PetType type;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "pet_id", nullable = false)
	@OrderBy("date ASC")
	private final Set<Visit> visits = new LinkedHashSet<>();

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public LocalDate getBirthDate() {
		return this.birthDate;
	}

	public PetType getType() {
		return this.type;
	}

	public void setType(PetType type) {
		this.type = type;
	}

	public Collection<Visit> getVisits() {
		return this.visits;
	}

	public void addVisit(Visit visit) {
		getVisits().add(visit);
	}

	public Visit getVisit(Integer id) {
		for (Visit visit : getVisits()) {
			if (!visit.isNew() && Objects.equals(visit.getId(), id)) {
				return visit;
			}
		}
		return null;
	}

	public void removeVisit(Visit visit) {
		getVisits().remove(visit);
	}

}
