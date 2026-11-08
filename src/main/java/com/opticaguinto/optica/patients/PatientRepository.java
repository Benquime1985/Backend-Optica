package com.opticaguinto.optica.patients;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Page<Patient> findByActiveTrueAndFirstNameContainingIgnoreCaseOrActiveTrueAndLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);
    Page<Patient> findByActiveTrue(Pageable pageable);
}
