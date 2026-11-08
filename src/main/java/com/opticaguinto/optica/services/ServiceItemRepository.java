package com.opticaguinto.optica.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
    Page<ServiceItem> findByActiveTrue(Pageable pageable);
    Page<ServiceItem> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);
}
