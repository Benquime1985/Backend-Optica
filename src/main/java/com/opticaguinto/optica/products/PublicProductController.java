package com.opticaguinto.optica.products;

import com.opticaguinto.optica.shared.pagination.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/products")
public class PublicProductController {
    private final ProductService service;
    public PublicProductController(ProductService service) { this.service = service; }

    @GetMapping
    public PageResponse<ProductDtos.PublicProductResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
        return PageResponse.from(service.searchCatalogo(q, pageable).map(ProductDtos.PublicProductResponse::from));
    }
}
