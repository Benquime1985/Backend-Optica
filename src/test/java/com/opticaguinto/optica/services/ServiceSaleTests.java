package com.opticaguinto.optica.services;

import com.opticaguinto.optica.auth.AuthDtos;
import com.opticaguinto.optica.auth.AuthService;
import com.opticaguinto.optica.consultations.ConsultationRepository;
import com.opticaguinto.optica.patients.PatientRepository;
import com.opticaguinto.optica.products.Product;
import com.opticaguinto.optica.products.ProductDtos;
import com.opticaguinto.optica.products.ProductRepository;
import com.opticaguinto.optica.products.ProductService;
import com.opticaguinto.optica.sales.SaleDtos;
import com.opticaguinto.optica.sales.SaleRepository;
import com.opticaguinto.optica.sales.SaleService;
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
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ServiceSaleTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ServiceItemService shopService;
    @Autowired private ServiceItemRepository shopRepository;
    @Autowired private SaleService saleService;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private PatientRepository patientRepository;

    @BeforeEach
    void cleanAll() {
        consultationRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        shopRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String login(String email, String password) {
        return authService.login(new AuthDtos.LoginRequest(email, password)).accessToken();
    }

    @Test
    void serviceCrudAndMixedSaleWithoutTouchingStock() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Admin", "admin@test.local", "password-123", Role.ADMIN));
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        userService.create(new UserDtos.CreateUserRequest("Opto", "opto@test.local", "password-123", Role.OPTOMETRISTA));
        String admin = login("admin@test.local", "password-123");
        String rece = login("rece@test.local", "password-123");
        String opto = login("opto@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/services")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Limpieza de lentes","description":"Limpieza y ajuste básico","category":"Mantenimiento","price":80.00}
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Limpieza de lentes"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.price").value(80.00));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/services")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Otro","price":10.00}
                                """))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/services")
                        .header("Authorization", "Bearer " + rece))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/services")
                        .header("Authorization", "Bearer " + opto))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        ServiceItem svc = shopRepository.findAll().iterator().next();
        Product p = productService.create(new ProductDtos.ProductRequest(
                "LIM-001", "Limpiador", null, "Limpieza", null, null, new BigDecimal("50.00"), 5, true));
        int stockBefore = productRepository.findById(p.getId()).orElseThrow().getStock();

        SaleDtos.SaleResponse sale = saleService.create(
                new SaleDtos.CreateSaleRequest(null, null,
                        List.of(new SaleDtos.SaleItemRequest(p.getId(), null, 1),
                                new SaleDtos.SaleItemRequest(null, svc.getId(), 1)), null),
                "rece@test.local");

        org.assertj.core.api.Assertions.assertThat(sale.total()).isEqualByComparingTo("130.00");
        org.assertj.core.api.Assertions.assertThat(sale.items().get(0).itemType()).isEqualTo(SaleDtos.ItemType.PRODUCT);
        org.assertj.core.api.Assertions.assertThat(sale.items().get(1).itemType()).isEqualTo(SaleDtos.ItemType.SERVICE);
        org.assertj.core.api.Assertions.assertThat(sale.items().get(1).serviceId()).isEqualTo(svc.getId());
        org.assertj.core.api.Assertions.assertThat(sale.items().get(1).productId()).isNull();

        int stockAfter = productRepository.findById(p.getId()).orElseThrow().getStock();
        org.assertj.core.api.Assertions.assertThat(stockAfter).isEqualTo(stockBefore - 1);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/public/products"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].name").value("Limpiador"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].name").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Limpieza de lentes"))));

        // xor: ambos nulos rechazado
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":null,"discount":0,"items":[{"productId":null,"serviceId":null,"quantity":1}]}
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        // servicio desactivado no vendible
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/services/" + svc.getId())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":null,"discount":0,"items":[{"serviceId":"%s","quantity":1}]}
                                """.formatted(svc.getId())))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
