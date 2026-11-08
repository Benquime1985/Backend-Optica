package com.opticaguinto.optica.services;

import com.opticaguinto.optica.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ServiceItemService {
    private final ServiceItemRepository repository;
    public ServiceItemService(ServiceItemRepository repository) { this.repository = repository; }

    @Transactional
    public ServiceItem create(ServiceItemDtos.ServiceRequest request) {
        ServiceItem service = new ServiceItem();
        copy(request, service);
        return repository.save(service);
    }

    @Transactional
    public ServiceItem update(UUID id, ServiceItemDtos.ServiceRequest request) {
        ServiceItem service = getActive(id);
        copy(request, service);
        return repository.save(service);
    }

    @Transactional(readOnly = true)
    public Page<ServiceItem> search(String query, Pageable pageable) {
        return query == null || query.isBlank() ? repository.findByActiveTrue(pageable)
                : repository.findByActiveTrueAndNameContainingIgnoreCase(query.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public ServiceItem getActive(UUID id) {
        ServiceItem service = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        if (!service.isActive()) throw new ResourceNotFoundException("Servicio no encontrado");
        return service;
    }

    @Transactional
    public void deactivate(UUID id) { getActive(id).setActive(false); }

    private void copy(ServiceItemDtos.ServiceRequest request, ServiceItem s) {
        s.setName(request.name().trim());
        s.setDescription(request.description());
        s.setCategory(request.category() == null || request.category().isBlank() ? null : request.category().trim());
        s.setPrice(request.price());
    }
}
