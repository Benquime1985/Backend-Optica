package com.opticaguinto.optica.sales;

import com.opticaguinto.optica.shared.pagination.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
public class SaleController {
    private final SaleService service;
    public SaleController(SaleService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleDtos.SaleResponse create(@Valid @RequestBody SaleDtos.CreateSaleRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return service.create(request, jwt.getSubject());
    }

    @GetMapping
    public PageResponse<SaleDtos.SaleResponse> list(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) String status,
                                                    Pageable pageable) {
        return PageResponse.from(service.search(q, status, pageable));
    }

    @GetMapping("/{id}")
    public SaleDtos.SaleResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/pago")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleDtos.SaleResponse registerPayment(@PathVariable UUID id,
                                                 @Valid @RequestBody SaleDtos.PaymentRequest request) {
        return service.registerPayment(id, request);
    }

    @PostMapping("/{id}/items")
    public SaleDtos.SaleResponse addItems(@PathVariable UUID id,
                                          @Valid @RequestBody SaleDtos.AddItemsRequest request) {
        return service.addItems(id, request);
    }

    @PostMapping("/{id}/entregar")
    public SaleDtos.SaleResponse deliver(@PathVariable UUID id) {
        return service.deliver(id);
    }

    @PostMapping("/{id}/factura")
    public SaleDtos.SaleResponse saveInvoice(@PathVariable UUID id,
                                             @Valid @RequestBody SaleDtos.InvoiceRequest request) {
        return service.saveInvoice(id, request);
    }
}
