package org.springframework.samples.petclinic.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialties")
@AttributeOverride(name = "name", column = @Column(name = "specialty_name"))
public class Specialty extends NamedEntity {
}
