package com.opticaguinto.optica.products;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ProductDtos {
    private ProductDtos() {}
    public record ProductRequest(@NotBlank String sku, @NotBlank String name, String description,
                                 String category, UUID categoryId, @Size(max = 1000) String imageUrl,
                                 @NotNull @DecimalMin("0.00") BigDecimal price,
                                 @Min(0) int stock, Boolean catalogo) {}
    public record ProductResponse(UUID id, String sku, String name, String description,
                                  UUID categoryId, String category, String imageUrl,
                                  BigDecimal price, int stock, boolean active, boolean catalogo, Instant createdAt, Instant updatedAt) {
        public static ProductResponse from(Product p) {
            ProductCategory c = p.getCategory();
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                    c == null ? null : c.getId(), c == null ? null : c.getName(),
                    p.getImageUrl(), p.getPrice(), p.getStock(), p.isActive(), p.isCatalogo(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }
    public record PublicProductResponse(UUID id, String name, String category, String imageUrl, BigDecimal price, int stock) {
        public static PublicProductResponse from(Product p) {
            return new PublicProductResponse(p.getId(), p.getName(),
                    p.getCategory() == null ? null : p.getCategory().getName(),
                    p.getImageUrl(), p.getPrice(), p.getStock());
        }
    }
}
