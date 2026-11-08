package com.opticaguinto.optica.consultations;

import com.opticaguinto.optica.patients.Patient;
import com.opticaguinto.optica.patients.PatientDtos;
import com.opticaguinto.optica.patients.PatientRepository;
import com.opticaguinto.optica.patients.PatientService;
import com.opticaguinto.optica.users.Role;
import com.opticaguinto.optica.users.User;
import com.opticaguinto.optica.users.UserDtos;
import com.opticaguinto.optica.users.UserRepository;
import com.opticaguinto.optica.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConsultationLazyLoadingTests {
    @Autowired private ConsultationService consultationService;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private PatientService patientService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanData() {
        consultationRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void consultationResponseIsMappedBeforeSessionCloses() {
        User professional = userService.create(new UserDtos.CreateUserRequest(
                "Optometrista", "optometrista@test.local", "password-123", Role.OPTOMETRISTA));
        Patient patient = patientService.create(new PatientDtos.PatientRequest(
                "Ana", "López", null, "5550000000", null, null, null));
        consultationService.create(patient.getId(), professional.getEmail(),
                new ConsultationDtos.ConsultationRequest(null, "Revisión visual", null, null, null, null, null));

        List<ConsultationDtos.ConsultationResponse> consultations = consultationService.findByPatient(patient.getId());

        assertThat(consultations).hasSize(1);
        assertThat(consultations.getFirst().professionalName()).isEqualTo("Optometrista");
    }
}
