package com.opticaguinto.optica.sales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SalePaymentRepository extends JpaRepository<SalePayment, UUID> {
}
