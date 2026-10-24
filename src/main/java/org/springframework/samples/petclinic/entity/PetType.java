package org.springframework.samples.petclinic.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "types")
@AttributeOverride(name = "name", column = @Column(name = "type_name"))
public class PetType extends NamedEntity {
}
