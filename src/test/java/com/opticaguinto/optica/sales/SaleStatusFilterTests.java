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

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SaleStatusFilterTests {
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

    private Product lente(String sku) {
        return productService.create(new ProductDtos.ProductRequest(
                sku, "Lente " + sku, null, "Lentes", null, null, new BigDecimal("100.00"), 10, null));
    }

    @Test
    void statusFilterReturnsOnlyMatchingSales() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        var patient = patientService.create(new PatientDtos.PatientRequest("Zulma", "Filtro", null, null, null, null, null));
        Product p1 = lente("LENTE-401");
        Product p2 = lente("LENTE-402");
        Product p3 = lente("LENTE-403");

        saleService.create(new SaleDtos.CreateSaleRequest(null, null,
                List.of(new SaleDtos.SaleItemRequest(p1.getId(), null, 1)),
                new SaleDtos.PaymentRequest(new BigDecimal("50.00"), PaymentMethod.EFECTIVO)), "rece@test.local");
        saleService.create(new SaleDtos.CreateSaleRequest(patient.getId(), null,
                List.of(new SaleDtos.SaleItemRequest(p2.getId(), null, 1)),
                new SaleDtos.PaymentRequest(new BigDecimal("100.00"), PaymentMethod.TARJETA)), "rece@test.local");
        saleService.create(new SaleDtos.CreateSaleRequest(null, null,
                List.of(new SaleDtos.SaleItemRequest(p3.getId(), null, 1)), null), "rece@test.local");
        String rece = login("rece@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .param("status", "PENDIENTE"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].status").value("PENDIENTE"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .param("status", "PAGADA_POR_ENTREGAR"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].status").value("PAGADA_POR_ENTREGAR"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .param("status", "COMPLETADA"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].status").value("COMPLETADA"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(3));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .param("q", "zulma")
                        .param("status", "PENDIENTE"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .param("q", "zulma")
                        .param("status", "PAGADA_POR_ENTREGAR"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .param("status", "INVALIDO"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Estado de venta inválido"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .param("status", "pendiente"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));
    }
}
