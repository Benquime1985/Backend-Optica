package com.opticaguinto.optica.products;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class CategoryDtos {
    private CategoryDtos() {}

    public record CategoryRequest(@NotBlank String name, @Min(0) Integer orden) {}

    public record CategoryResponse(UUID id, String name, int orden, boolean active) {
        public static CategoryResponse from(ProductCategory c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getOrden(), c.isActive());
        }
    }
}
