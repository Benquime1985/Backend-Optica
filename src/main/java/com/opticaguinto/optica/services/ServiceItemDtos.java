package com.opticaguinto.optica.services;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ServiceItemDtos {
    private ServiceItemDtos() {}

    public record ServiceRequest(@NotBlank String name, String description, String category,
                                 @NotNull @DecimalMin("0.00") BigDecimal price) {}

    public record ServiceResponse(UUID id, String name, String description, String category,
                                  BigDecimal price, boolean active, Instant createdAt, Instant updatedAt) {
        public static ServiceResponse from(ServiceItem s) {
            return new ServiceResponse(s.getId(), s.getName(), s.getDescription(), s.getCategory(),
                    s.getPrice(), s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
        }
    }
}
