package com.opticaguinto.optica.consultations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {
    List<Consultation> findByPatientIdOrderByConsultationDateDesc(UUID patientId);
}
