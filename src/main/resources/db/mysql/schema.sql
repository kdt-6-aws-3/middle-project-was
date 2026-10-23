-- 1. 병원
CREATE TABLE hospitals (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    hospital_name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,

    CONSTRAINT uq_hospitals_phone UNIQUE (phone),
    CONSTRAINT chk_hospitals_phone_format
        CHECK (phone REGEXP '^[0-9]{2,3}-[0-9]{3,4}-[0-9]{4}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 수의사
CREATE TABLE vets (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    hospital_id INT UNSIGNED NOT NULL,
    vet_name VARCHAR(255) NOT NULL,

    CONSTRAINT fk_vets_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 전문 분야
CREATE TABLE specialties (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    specialty_name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 수의사별 전문 분야
CREATE TABLE vet_specialties (
    vet_id INT UNSIGNED NOT NULL,
    specialty_id INT UNSIGNED NOT NULL,

    CONSTRAINT fk_vet_specialties_vet
        FOREIGN KEY (vet_id) REFERENCES vets(id),
    CONSTRAINT fk_vet_specialties_specialty
        FOREIGN KEY (specialty_id) REFERENCES specialties(id),
    CONSTRAINT uq_vet_specialties UNIQUE (vet_id, specialty_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 보호자
CREATE TABLE owners (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,

    CONSTRAINT uq_owners_phone UNIQUE (phone),
    CONSTRAINT chk_owners_phone_format
        CHECK (phone REGEXP '^[0-9]{2,3}-[0-9]{3,4}-[0-9]{4}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 동물 종류
CREATE TABLE types (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 반려동물
CREATE TABLE pets (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type_id INT UNSIGNED NOT NULL,
    owner_id INT UNSIGNED NOT NULL,
    pet_name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,

    CONSTRAINT fk_pets_type
        FOREIGN KEY (type_id) REFERENCES types(id),
    CONSTRAINT fk_pets_owner
        FOREIGN KEY (owner_id) REFERENCES owners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 방문·진료 기록
CREATE TABLE visits (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pet_id INT UNSIGNED NOT NULL,
    visit_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    prescription VARCHAR(255) NULL,

    CONSTRAINT fk_visits_pet
        FOREIGN KEY (pet_id) REFERENCES pets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. 진료 예약
CREATE TABLE appointments (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    hospital_id INT UNSIGNED NOT NULL,
    pet_id INT UNSIGNED NOT NULL,
    vet_id INT UNSIGNED NOT NULL,
    appointment_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 취소된 예약은 슬롯을 해제하여 같은 시간에 다시 예약할 수 있도록 함
    active_slot TINYINT GENERATED ALWAYS AS (IF(status = 'CANCELLED', NULL, 1)) STORED,
    UNIQUE KEY uk_appointments_vet_slot (vet_id, appointment_at, active_slot),
    UNIQUE KEY uk_appointments_pet_slot (pet_id, appointment_at, active_slot),

    CONSTRAINT fk_appointments_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    CONSTRAINT fk_appointments_pet
        FOREIGN KEY (pet_id) REFERENCES pets(id),
    CONSTRAINT fk_appointments_vet
        FOREIGN KEY (vet_id) REFERENCES vets(id),

    CONSTRAINT chk_appointment_status
        CHECK (status IN ('REQUESTED', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),

    INDEX idx_appointments_hospital_time
        (hospital_id, appointment_at),
    INDEX idx_appointments_vet_time
        (vet_id, appointment_at),
    INDEX idx_appointments_pet_time
        (pet_id, appointment_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
