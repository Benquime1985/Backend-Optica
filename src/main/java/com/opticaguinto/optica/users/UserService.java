package com.opticaguinto.optica.users;

import com.opticaguinto.optica.shared.exception.BusinessException;
import com.opticaguinto.optica.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(UserDtos.CreateUserRequest request) {
        String email = normalize(request.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("El correo ya está registrado");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        return repository.save(user);
    }

    @Transactional
    public User update(UUID id, UserDtos.UpdateUserRequest request, String currentEmail) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        String email = normalize(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("El correo ya está registrado");
        }
        boolean proposedActive = request.active() == null ? user.isActive() : request.active();
        guardNotSelfNorLastAdmin(user, request.role(), proposedActive, currentEmail);
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setActive(proposedActive);
        return repository.save(user);
    }

    @Transactional
    public void deactivate(UUID id, String currentEmail) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        guardNotSelfNorLastAdmin(user, user.getRole(), false, currentEmail);
        user.setActive(false);
        repository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<User> search(String query, Pageable pageable) {
        return query == null || query.isBlank()
                ? repository.findAll(pageable)
                : repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        query.trim(), query.trim(), pageable);
    }

    private void guardNotSelfNorLastAdmin(User target, Role proposedRole, boolean proposedActive, String currentEmail) {
        if (proposedActive && proposedRole == Role.ADMIN) return;
        if (!target.isActive() || target.getRole() != Role.ADMIN) return;
        if (normalize(target.getEmail()).equals(normalize(currentEmail))) {
            throw new BusinessException("No puedes desactivar ni degradar tu propia cuenta");
        }
        if (repository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new BusinessException("No se puede desactivar ni degradar al último administrador activo");
        }
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return repository.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    public long count() {
        return repository.count();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
