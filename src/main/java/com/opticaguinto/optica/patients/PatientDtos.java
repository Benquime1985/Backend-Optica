package com.opticaguinto.optica.patients;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class PatientDtos {
    private PatientDtos() {}

    public record PatientRequest(@NotBlank String firstName, @NotBlank String lastName,
                                 LocalDate birthDate, String phone, String email,
                                 String address, String notes) {}

    public record PatientResponse(UUID id, String firstName, String lastName, LocalDate birthDate,
                                  String phone, String email, String address, String notes,
                                  boolean active, Instant createdAt, Instant updatedAt) {
        public static PatientResponse from(Patient p) {
            return new PatientResponse(p.getId(), p.getFirstName(), p.getLastName(), p.getBirthDate(),
                    p.getPhone(), p.getEmail(), p.getAddress(), p.getNotes(), p.isActive(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
