package com.opticaguinto.optica.products;

import com.opticaguinto.optica.consultations.ConsultationRepository;
import com.opticaguinto.optica.sales.SaleRepository;
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
class PublicProductTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private SaleRepository saleRepository;

    @BeforeEach
    void cleanProducts() {
        consultationRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void publicListExposesOnlyCatalogFieldsAndHidesInactive() throws Exception {
        Product visible = productService.create(new ProductDtos.ProductRequest(
                "LENTE-001", "Lente monofocal", null, "Lentes", null,
                "https://cdn.example.com/lente.jpg", new BigDecimal("899.50"), 5, true));
        Product hidden = productService.create(new ProductDtos.ProductRequest(
                "LENTE-002", "Lente secreta", null, "Lentes", null,
                null, new BigDecimal("500.00"), 3, true));
        productService.deactivate(hidden.getId());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/public/products"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(visible.getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].name").value("Lente monofocal"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].category").value("Lentes"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].price").value(899.50))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].imageUrl").value("https://cdn.example.com/lente.jpg"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].stock").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].sku").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].description").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].active").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));

        // search por nombre sigue funcionando sin token
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/public/products").param("q", "monofocal"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));
    }
}
