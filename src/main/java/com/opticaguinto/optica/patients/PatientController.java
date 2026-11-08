package com.opticaguinto.optica.patients;

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
@RequestMapping("/api/v1/patients")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCION','OPTOMETRISTA')")
public class PatientController {
    private final PatientService service;
    public PatientController(PatientService service) { this.service = service; }

    @GetMapping
    public PageResponse<PatientDtos.PatientResponse> search(@RequestParam(required = false) String q, Pageable pageable) {
        return PageResponse.from(service.search(q, pageable).map(PatientDtos.PatientResponse::from));
    }

    @GetMapping("/{id}")
    public PatientDtos.PatientResponse get(@PathVariable UUID id) {
        return PatientDtos.PatientResponse.from(service.getActive(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDtos.PatientResponse create(@Valid @RequestBody PatientDtos.PatientRequest request) {
        return PatientDtos.PatientResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public PatientDtos.PatientResponse update(@PathVariable UUID id,
                                              @Valid @RequestBody PatientDtos.PatientRequest request) {
        return PatientDtos.PatientResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    public void deactivate(@PathVariable UUID id) { service.deactivate(id); }
}
