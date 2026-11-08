package com.opticaguinto.optica.pagination;

import com.opticaguinto.optica.auth.AuthDtos;
import com.opticaguinto.optica.auth.AuthService;
import com.opticaguinto.optica.products.ProductDtos;
import com.opticaguinto.optica.products.ProductRepository;
import com.opticaguinto.optica.products.ProductService;
import com.opticaguinto.optica.consultations.ConsultationRepository;
import com.opticaguinto.optica.sales.SaleRepository;
import com.opticaguinto.optica.patients.PatientRepository;
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

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PaginationContractTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private PatientRepository patientRepository;

    @BeforeEach
    void clean() {
        consultationRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void paginatedEndpointsExposeBothLegacyAndPageNode() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Admin", "admin@test.local", "password-123", Role.ADMIN));
        productService.create(new ProductDtos.ProductRequest(
                "LEN-990", "Lente contrato", null, "Lentes", null, null, new BigDecimal("100.00"), 5, true));
        String admin = authService.login(new AuthDtos.LoginRequest("admin@test.local", "password-123")).accessToken();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].sku").value("LEN-990"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.number").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page.totalPages").value(1));
    }
}
