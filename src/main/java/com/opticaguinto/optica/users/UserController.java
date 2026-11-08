package com.opticaguinto.optica.users;

import com.opticaguinto.optica.shared.pagination.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserDtos.UserResponse create(@Valid @RequestBody UserDtos.CreateUserRequest request) {
        return UserDtos.UserResponse.from(service.create(request));
    }

    @GetMapping("/me")
    public UserDtos.UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserDtos.UserResponse.from(service.getByEmail(jwt.getSubject()));
    }

    @GetMapping
    public PageResponse<UserDtos.UserResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
        return PageResponse.from(service.search(q, pageable).map(UserDtos.UserResponse::from));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDtos.UserResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody UserDtos.UpdateUserRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return UserDtos.UserResponse.from(service.update(id, request, jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.deactivate(id, jwt.getSubject());
    }
}
