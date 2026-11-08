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
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CategoryTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductCategoryRepository categoryRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private SaleRepository saleRepository;

    @BeforeEach
    void cleanAll() {
        consultationRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String login(String email, String password) {
        return authService.login(new AuthDtos.LoginRequest(email, password)).accessToken();
    }

    @Test
    void categoriesListOrderedAndProductUsesCategoryId() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Admin", "admin@test.local", "password-123", Role.ADMIN));
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        String admin = login("admin@test.local", "password-123");
        String rece = login("rece@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/categories")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Servicios","orden":2}
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.orden").value(2));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/categories")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Lentes","orden":1}
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.orden").value(1));

        // duplicado ignore case
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/categories")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"lentes"}
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        // RECEPCION no puede crear
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/categories")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Otro","orden":3}
                                """))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/categories")
                        .header("Authorization", "Bearer " + rece))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Lentes"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Servicios"));

        ProductCategory lentes = categoryRepository.findByNameIgnoreCase("Lentes").orElseThrow();
        Product p = productService.create(new ProductDtos.ProductRequest(
                "LEN-901", "Lente con categoria", null, null, lentes.getId(), null, new BigDecimal("100.00"), 5, true));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/products")
                        .header("Authorization", "Bearer " + admin)
                        .param("q", "categoria"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].categoryId").value(lentes.getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].category").value("Lentes"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/public/products")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].category").value("Lentes"));

        // categoryId inexistente
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"BAD-1","name":"Bad","description":null,"category":null,"categoryId":"%s","imageUrl":null,"price":10,"stock":1,"catalogo":true}
                                """.formatted(UUID.randomUUID())))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
