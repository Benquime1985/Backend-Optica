package com.opticaguinto.optica.products;

import com.opticaguinto.optica.shared.exception.BusinessException;
import com.opticaguinto.optica.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final ProductCategoryRepository categories;
    public ProductService(ProductRepository repository, ProductCategoryRepository categories) {
        this.repository = repository; this.categories = categories;
    }

    @Transactional
    public Product create(ProductDtos.ProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (repository.findBySkuIgnoreCase(sku).isPresent()) throw new BusinessException("El SKU ya está registrado");
        Product product = new Product();
        copy(request, product);
        product.setSku(sku);
        return repository.save(product);
    }

    @Transactional
    public Product update(UUID id, ProductDtos.ProductRequest request) {
        Product product = getActive(id);
        String sku = normalizeSku(request.sku());
        repository.findBySkuIgnoreCase(sku).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new BusinessException("El SKU ya está registrado"); });
        copy(request, product);
        product.setSku(sku);
        return repository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> search(String query, Pageable pageable) {
        return query == null || query.isBlank() ? repository.findByActiveTrue(pageable)
                : repository.findByActiveTrueAndNameContainingIgnoreCase(query.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchCatalogo(String query, Pageable pageable) {
        return query == null || query.isBlank() ? repository.findByActiveTrueAndCatalogoTrue(pageable)
                : repository.findByActiveTrueAndCatalogoTrueAndNameContainingIgnoreCase(query.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Product getActive(UUID id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        if (!product.isActive()) throw new ResourceNotFoundException("Producto no encontrado");
        return product;
    }

    @Transactional
    public void deactivate(UUID id) { getActive(id).setActive(false); }

    @Transactional
    public Product reserve(UUID id, int quantity) {
        Product product = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        if (!product.isActive()) throw new BusinessException("El producto está inactivo");
        if (product.getStock() < quantity) throw new BusinessException("Existencia insuficiente para " + product.getName());
        product.setStock(product.getStock() - quantity);
        return product;
    }

    private void copy(ProductDtos.ProductRequest request, Product p) {
        p.setName(request.name().trim()); p.setDescription(request.description());
        resolveCategory(request, p);
        String imageUrl = request.imageUrl() == null ? null : request.imageUrl().trim();
        p.setImageUrl(imageUrl == null || imageUrl.isBlank() ? null : imageUrl);
        p.setPrice(request.price()); p.setStock(request.stock());
        if (request.catalogo() != null) p.setCatalogo(request.catalogo());
    }

    private void resolveCategory(ProductDtos.ProductRequest request, Product p) {
        if (request.categoryId() != null) {
            ProductCategory category = categories.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
            if (!category.isActive()) throw new ResourceNotFoundException("Categoría no encontrada");
            p.setCategory(category);
            return;
        }
        if (request.category() != null && !request.category().isBlank()) {
            String name = request.category().trim();
            ProductCategory category = categories.findByNameIgnoreCase(name).orElseGet(() -> {
                ProductCategory created = new ProductCategory();
                created.setName(name);
                created.setOrden(0);
                created.setActive(true);
                return categories.save(created);
            });
            if (!category.isActive()) category.setActive(true);
            p.setCategory(category);
            return;
        }
        if (p.getId() == null) p.setCategory(null);
    }
    private String normalizeSku(String sku) { return sku.trim().toUpperCase(Locale.ROOT); }
}
