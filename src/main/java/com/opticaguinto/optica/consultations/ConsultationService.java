package com.opticaguinto.optica.consultations;

import com.opticaguinto.optica.patients.Patient;
import com.opticaguinto.optica.patients.PatientService;
import com.opticaguinto.optica.users.User;
import com.opticaguinto.optica.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ConsultationService {
    private final ConsultationRepository repository;
    private final PatientService patients;
    private final UserService users;

    public ConsultationService(ConsultationRepository repository, PatientService patients, UserService users) {
        this.repository = repository; this.patients = patients; this.users = users;
    }

    @Transactional
    public ConsultationDtos.ConsultationResponse create(UUID patientId, String professionalEmail, ConsultationDtos.ConsultationRequest request) {
        Patient patient = patients.getActive(patientId);
        User professional = users.getByEmail(professionalEmail);
        Consultation consultation = new Consultation();
        consultation.setPatient(patient);
        consultation.setProfessional(professional);
        consultation.setConsultationDate(request.consultationDate() == null ? LocalDateTime.now() : request.consultationDate());
        consultation.setReason(request.reason().trim());
        consultation.setVisualAcuity(request.visualAcuity());
        consultation.setGraduation(request.graduation());
        consultation.setObservations(request.observations());
        consultation.setRecommendations(request.recommendations());
        consultation.setNextReviewAt(request.nextReviewAt());
        return ConsultationDtos.ConsultationResponse.from(repository.save(consultation));
    }

    @Transactional(readOnly = true)
    public List<ConsultationDtos.ConsultationResponse> findByPatient(UUID patientId) {
        patients.getActive(patientId);
        return repository.findByPatientIdOrderByConsultationDateDesc(patientId).stream()
                .map(ConsultationDtos.ConsultationResponse::from)
                .toList();
    }
}
