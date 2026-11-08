package com.opticaguinto.optica.auth;

import com.opticaguinto.optica.users.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8) String password) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds,
                                Role role) {}
}
