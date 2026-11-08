package com.opticaguinto.optica.products;

import com.opticaguinto.optica.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final ProductCategoryRepository repository;
    public CategoryService(ProductCategoryRepository repository) { this.repository = repository; }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CategoryRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) throw new BusinessException("La categoría ya está registrada");
        ProductCategory category = new ProductCategory();
        category.setName(name);
        category.setOrden(request.orden() == null ? 0 : request.orden());
        category.setActive(true);
        return CategoryDtos.CategoryResponse.from(repository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> list() {
        return repository.findAllByActiveTrueOrderByOrdenAscNameAsc().stream()
                .map(CategoryDtos.CategoryResponse::from).toList();
    }
}
