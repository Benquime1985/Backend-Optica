package com.opticaguinto.optica.sales;

import com.opticaguinto.optica.auth.AuthDtos;
import com.opticaguinto.optica.auth.AuthService;
import com.opticaguinto.optica.consultations.ConsultationRepository;
import com.opticaguinto.optica.patients.PatientDtos;
import com.opticaguinto.optica.patients.PatientRepository;
import com.opticaguinto.optica.patients.PatientService;
import com.opticaguinto.optica.products.Product;
import com.opticaguinto.optica.products.ProductDtos;
import com.opticaguinto.optica.products.ProductRepository;
import com.opticaguinto.optica.products.ProductService;
import com.opticaguinto.optica.users.Role;
import com.opticaguinto.optica.users.UserDtos;
import com.opticaguinto.optica.users.UserRepository;
import com.opticaguinto.optica.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PatientSaleTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientService patientService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private SaleService saleService;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ConsultationRepository consultationRepository;

    @BeforeEach
    void cleanAll() {
        consultationRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String login(String email, String password) {
        return authService.login(new AuthDtos.LoginRequest(email, password)).accessToken();
    }

    private Product lente(String sku, int stock) {
        return productService.create(new ProductDtos.ProductRequest(
                sku, "Lente " + sku, null, "Lentes", null, null, new BigDecimal("100.00"), stock, null));
    }

    @Test
    void returnsExactPatientSalesPaginated() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        var p1 = patientService.create(new PatientDtos.PatientRequest("Ana", "Uniquemira", null, null, null, null, null));
        var p2 = patientService.create(new PatientDtos.PatientRequest("Ana", "Otra", null, null, null, null, null));
        Product prod1 = lente("LENTE-201", 10);
        Product prod2 = lente("LENTE-202", 10);

        saleService.create(new SaleDtos.CreateSaleRequest(p1.getId(), null, List.of(new SaleDtos.SaleItemRequest(prod1.getId(), null, 1)), null), "rece@test.local");
        saleService.create(new SaleDtos.CreateSaleRequest(p2.getId(), null, List.of(new SaleDtos.SaleItemRequest(prod2.getId(), null, 1)), null), "rece@test.local");
        String rece = login("rece@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/patients/" + p1.getId() + "/sales")
                        .header("Authorization", "Bearer " + rece))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].patientId").value(p1.getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].items[0].productId").value(prod1.getId().toString()));
    }

    @Test
    void missingPatientReturns404AndOptometristaForbidden() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        userService.create(new UserDtos.CreateUserRequest("Opto", "opto@test.local", "password-123", Role.OPTOMETRISTA));
        var p1 = patientService.create(new PatientDtos.PatientRequest("Ana", "Uniquemira", null, null, null, null, null));
        String rece = login("rece@test.local", "password-123");
        String opto = login("opto@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/patients/" + UUID.randomUUID() + "/sales")
                        .header("Authorization", "Bearer " + rece))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Paciente no encontrado"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/patients/" + p1.getId() + "/sales")
                        .header("Authorization", "Bearer " + opto))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/patients/" + p1.getId() + "/sales"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
