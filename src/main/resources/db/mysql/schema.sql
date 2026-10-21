CREATE TABLE hospitals (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    hospital_name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vets (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    hospital_id INT UNSIGNED NOT NULL,
    vet_name VARCHAR(255) NOT NULL,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE specialties (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    specialty_name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vet_specialties (
    vet_id INT UNSIGNED NOT NULL,
    specialty_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (vet_id) REFERENCES vets(id),
    FOREIGN KEY (specialty_id) REFERENCES specialties(id),
    UNIQUE (vet_id, specialty_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE owners (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE types (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pets (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type_id INT UNSIGNED NOT NULL,
    owner_id INT UNSIGNED NOT NULL,
    pet_name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    FOREIGN KEY (type_id) REFERENCES types(id),
    FOREIGN KEY (owner_id) REFERENCES owners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE visits (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pet_id INT UNSIGNED NOT NULL,
    visit_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    FOREIGN KEY (pet_id) REFERENCES pets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;