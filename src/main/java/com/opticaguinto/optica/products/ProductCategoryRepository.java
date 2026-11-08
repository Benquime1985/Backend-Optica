package com.opticaguinto.optica.products;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
    List<ProductCategory> findAllByActiveTrueOrderByOrdenAscNameAsc();
    Optional<ProductCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
