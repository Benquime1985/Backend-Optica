package com.opticaguinto.optica.auth;

import com.opticaguinto.optica.users.Role;
import com.opticaguinto.optica.users.UserDtos;
import com.opticaguinto.optica.users.UserRepository;
import com.opticaguinto.optica.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthFlowTests {
    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    void loginIssuesAccessToken() {
        userService.create(new UserDtos.CreateUserRequest(
                "Administrador", "admin@test.local", "password-123", Role.ADMIN));

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("admin@test.local", "password-123"));

        assertThat(response.accessToken()).isNotBlank();
        Jwt jwt = jwtDecoder.decode(response.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("admin@test.local");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
    }
}
