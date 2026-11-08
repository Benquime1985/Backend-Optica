package com.opticaguinto.optica.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {
    private UserDtos() {}

    public record CreateUserRequest(@NotBlank String name, @NotBlank @Email String email,
                                    @NotBlank @Size(min = 8) String password, @NotNull Role role) {}

    // password/active nullables: null = keep current (PUT completo salvo esos dos)
    public record UpdateUserRequest(@NotBlank String name, @NotBlank @Email String email,
                                    @NotNull Role role, @Size(min = 8) String password, Boolean active) {}

    public record UserResponse(UUID id, String name, String email, Role role, boolean active,
                               Instant createdAt) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                    user.isActive(), user.getCreatedAt());
        }
    }
}
