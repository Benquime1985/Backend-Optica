package com.opticaguinto.optica.consultations;

import com.opticaguinto.optica.patients.Patient;
import com.opticaguinto.optica.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consultations")
public class Consultation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(nullable = false) private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "professional_id", nullable = false) private User professional;
    @Column(nullable = false) private LocalDateTime consultationDate;
    @Column(nullable = false, length = 500) private String reason;
    @Column(length = 1000) private String visualAcuity;
    @Column(length = 1000) private String graduation;
    @Column(length = 2000) private String observations;
    @Column(length = 2000) private String recommendations;
    private LocalDateTime nextReviewAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    @jakarta.persistence.PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public User getProfessional() { return professional; }
    public void setProfessional(User professional) { this.professional = professional; }
    public LocalDateTime getConsultationDate() { return consultationDate; }
    public void setConsultationDate(LocalDateTime consultationDate) { this.consultationDate = consultationDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getVisualAcuity() { return visualAcuity; }
    public void setVisualAcuity(String visualAcuity) { this.visualAcuity = visualAcuity; }
    public String getGraduation() { return graduation; }
    public void setGraduation(String graduation) { this.graduation = graduation; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public LocalDateTime getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(LocalDateTime nextReviewAt) { this.nextReviewAt = nextReviewAt; }
    public Instant getCreatedAt() { return createdAt; }
}
