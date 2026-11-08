package com.opticaguinto.optica.products;

import com.opticaguinto.optica.shared.pagination.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION','OPTOMETRISTA')")
    public PageResponse<ProductDtos.ProductResponse> search(@RequestParam(required = false) String q, Pageable pageable) {
        return PageResponse.from(service.search(q, pageable).map(ProductDtos.ProductResponse::from));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDtos.ProductResponse create(@Valid @RequestBody ProductDtos.ProductRequest request) {
        return ProductDtos.ProductResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDtos.ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductDtos.ProductRequest request) {
        return ProductDtos.ProductResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable UUID id) { service.deactivate(id); }
}
