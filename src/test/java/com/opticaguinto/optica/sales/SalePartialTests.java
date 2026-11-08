package com.opticaguinto.optica.sales;

import com.opticaguinto.optica.auth.AuthDtos;
import com.opticaguinto.optica.auth.AuthService;
import com.opticaguinto.optica.consultations.ConsultationRepository;
import com.opticaguinto.optica.patients.PatientRepository;
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
class SalePartialTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private SaleService saleService;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private PatientRepository patientRepository;

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

    private Product lente(String sku, String price) {
        return productService.create(new ProductDtos.ProductRequest(
                sku, "Lente " + sku, null, "Lentes", null, null, new BigDecimal(price), 10, null));
    }

    @Test
    void mixedTenderFlowTracksBreakdownAndRfc() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        Product p = lente("LENTE-301", "200.00");

        // crear con anticipo en efectivo (tarifa parcial)
        SaleDtos.SaleResponse sale = saleService.create(
                new SaleDtos.CreateSaleRequest(null, null,
                        List.of(new SaleDtos.SaleItemRequest(p.getId(), null, 1)),
                        new SaleDtos.PaymentRequest(new BigDecimal("80.00"), PaymentMethod.EFECTIVO)),
                "rece@test.local");
        org.assertj.core.api.Assertions.assertThat(sale.status()).isEqualTo(SaleStatus.PENDIENTE);
        org.assertj.core.api.Assertions.assertThat(sale.saldoPendiente()).isEqualByComparingTo("120.00");

        String rece = login("rece@test.local", "password-123");

        // segundo pago con tarjeta liquida el total pero queda por entregar
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales/" + sale.id() + "/pago")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":120.00,"metodo":"TARJETA"}
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("PAGADA_POR_ENTREGAR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalEfectivo").value(80.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalTarjeta").value(120.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPagado").value(200.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.saldoPendiente").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payments.length()").value(2));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales/" + sale.id() + "/entregar")
                        .header("Authorization", "Bearer " + rece))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("COMPLETADA"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales/" + sale.id() + "/factura")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rfc":"AAA010101AAA","numeroFactura":"F-100"}
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.rfc").value("AAA010101AAA"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.numeroFactura").value("F-100"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/sales")
                        .header("Authorization", "Bearer " + rece)
                        .param("q", "aaa010101aaa"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));
    }

    @Test
    void paymentOverTotalIsRejected() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        Product p = lente("LENTE-302", "100.00");
        SaleDtos.SaleResponse sale = saleService.create(
                new SaleDtos.CreateSaleRequest(null, null,
                        List.of(new SaleDtos.SaleItemRequest(p.getId(), null, 1)),
                        new SaleDtos.PaymentRequest(new BigDecimal("50.00"), PaymentMethod.EFECTIVO)),
                "rece@test.local");
        String rece = login("rece@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales/" + sale.id() + "/pago")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":60.00,"metodo":"EFECTIVO"}
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("El pago no puede superar el total de la venta"));
    }

    @Test
    void deliveredSaleBlocksPaymentsItemsAndRedelivery() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        Product p1 = lente("LENTE-303", "100.00");
        Product p2 = lente("LENTE-304", "100.00");
        SaleDtos.SaleResponse sale = saleService.create(
                new SaleDtos.CreateSaleRequest(null, null,
                        List.of(new SaleDtos.SaleItemRequest(p1.getId(), null, 1)),
                        new SaleDtos.PaymentRequest(new BigDecimal("100.00"), PaymentMethod.TARJETA)),
                "rece@test.local");
        org.assertj.core.api.Assertions.assertThat(sale.status()).isEqualTo(SaleStatus.PAGADA_POR_ENTREGAR);
        String rece = login("rece@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales/" + sale.id() + "/entregar")
                        .header("Authorization", "Bearer " + rece))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("COMPLETADA"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales/" + sale.id() + "/pago")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":10.00,"metodo":"EFECTIVO"}
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales/" + sale.id() + "/items")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":"%s","quantity":1}]}
                                """.formatted(p2.getId())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales/" + sale.id() + "/entregar")
                        .header("Authorization", "Bearer " + rece))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void addItemsToPendingRecalculatesTotalAndStatus() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        Product p1 = lente("LENTE-305", "100.00");
        Product p2 = lente("LENTE-306", "50.00");
        SaleDtos.SaleResponse sale = saleService.create(
                new SaleDtos.CreateSaleRequest(null, null,
                        List.of(new SaleDtos.SaleItemRequest(p1.getId(), null, 1)),
                        new SaleDtos.PaymentRequest(new BigDecimal("100.00"), PaymentMethod.EFECTIVO)),
                "rece@test.local");
        org.assertj.core.api.Assertions.assertThat(sale.status()).isEqualTo(SaleStatus.PAGADA_POR_ENTREGAR);
        String rece = login("rece@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/sales/" + sale.id() + "/items")
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":"%s","quantity":1}]}
                                """.formatted(p2.getId())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("PENDIENTE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.total").value(150.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.saldoPendiente").value(50.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(2));
    }
}
