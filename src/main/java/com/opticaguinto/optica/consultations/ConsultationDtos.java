package com.opticaguinto.optica.consultations;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ConsultationDtos {
    private ConsultationDtos() {}
    public record ConsultationRequest(LocalDateTime consultationDate, @NotBlank String reason,
                                      String visualAcuity, String graduation, String observations,
                                      String recommendations, LocalDateTime nextReviewAt) {}
    public record ConsultationResponse(UUID id, UUID patientId, UUID professionalId,
                                       String professionalName, LocalDateTime consultationDate,
                                       String reason, String visualAcuity, String graduation,
                                       String observations, String recommendations,
                                       LocalDateTime nextReviewAt, Instant createdAt) {
        public static ConsultationResponse from(Consultation c) {
            return new ConsultationResponse(c.getId(), c.getPatient().getId(), c.getProfessional().getId(),
                    c.getProfessional().getName(), c.getConsultationDate(), c.getReason(), c.getVisualAcuity(),
                    c.getGraduation(), c.getObservations(), c.getRecommendations(), c.getNextReviewAt(), c.getCreatedAt());
        }
    }
}
