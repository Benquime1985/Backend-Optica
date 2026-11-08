package com.opticaguinto.optica.products;

import com.opticaguinto.optica.auth.AuthDtos;
import com.opticaguinto.optica.auth.AuthService;
import com.opticaguinto.optica.consultations.ConsultationRepository;
import com.opticaguinto.optica.sales.SaleRepository;
import com.opticaguinto.optica.users.Role;
import com.opticaguinto.optica.users.UserDtos;
import com.opticaguinto.optica.users.UserRepository;
import com.opticaguinto.optica.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductCatalogoTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private SaleRepository saleRepository;

    @BeforeEach
    void cleanAll() {
        consultationRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String login(String email, String password) {
        return authService.login(new AuthDtos.LoginRequest(email, password)).accessToken();
    }

    @Test
    void publicCatalogHidesNonCatalogoButInternalShowsAll() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Admin", "admin@test.local", "password-123", Role.ADMIN));
        Product vitrina = productService.create(new ProductDtos.ProductRequest(
                "VIT-001", "Lente vitrina", null, "Lentes", null, null, new BigDecimal("100.00"), 5, true));
        Product servicio = productService.create(new ProductDtos.ProductRequest(
                "SRV-001", "Cambio de micas", null, "Servicios", null, null, new BigDecimal("150.00"), 5, false));
        String admin = login("admin@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/public/products"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].name").value("Lente vitrina"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/products/" + vitrina.getId())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"VIT-001","name":"Lente vitrina","description":null,"category":"Lentes","imageUrl":null,"price":100.00,"stock":5,"catalogo":false}
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.catalogo").value(false));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/public/products"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(2));
    }
}
