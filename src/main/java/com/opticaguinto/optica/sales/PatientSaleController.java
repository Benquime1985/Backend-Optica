package com.opticaguinto.optica.sales;

import com.opticaguinto.optica.shared.pagination.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/sales")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
public class PatientSaleController {
    private final SaleService service;
    public PatientSaleController(SaleService service) { this.service = service; }

    @GetMapping
    public PageResponse<SaleDtos.SaleResponse> list(@PathVariable UUID patientId, Pageable pageable) {
        return PageResponse.from(service.findByPatient(patientId, pageable));
    }
}
