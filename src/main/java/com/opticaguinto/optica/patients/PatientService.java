package com.opticaguinto.optica.patients;

import com.opticaguinto.optica.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PatientService {
    private final PatientRepository repository;

    public PatientService(PatientRepository repository) { this.repository = repository; }

    @Transactional
    public Patient create(PatientDtos.PatientRequest request) {
        Patient patient = new Patient();
        copy(request, patient);
        return repository.save(patient);
    }

    @Transactional
    public Patient update(UUID id, PatientDtos.PatientRequest request) {
        Patient patient = getActive(id);
        copy(request, patient);
        return repository.save(patient);
    }

    @Transactional(readOnly = true)
    public Patient getActive(UUID id) {
        Patient patient = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        if (!patient.isActive()) throw new ResourceNotFoundException("Paciente no encontrado");
        return patient;
    }

    @Transactional(readOnly = true)
    public Page<Patient> search(String query, Pageable pageable) {
        return query == null || query.isBlank()
                ? repository.findByActiveTrue(pageable)
                : repository.findByActiveTrueAndFirstNameContainingIgnoreCaseOrActiveTrueAndLastNameContainingIgnoreCase(
                        query.trim(), query.trim(), pageable);
    }

    @Transactional
    public void deactivate(UUID id) { getActive(id).setActive(false); }

    private void copy(PatientDtos.PatientRequest request, Patient patient) {
        patient.setFirstName(request.firstName().trim());
        patient.setLastName(request.lastName().trim());
        patient.setBirthDate(request.birthDate());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setAddress(request.address());
        patient.setNotes(request.notes());
    }
}
