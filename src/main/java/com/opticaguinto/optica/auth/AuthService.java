package com.opticaguinto.optica.auth;

import com.opticaguinto.optica.shared.exception.BusinessException;
import com.opticaguinto.optica.users.Role;
import com.opticaguinto.optica.users.User;
import com.opticaguinto.optica.users.UserDtos;
import com.opticaguinto.optica.users.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserService users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserService users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = users.getByEmail(request.email());
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Credenciales inválidas");
        }
        return jwtService.issue(user);
    }

    @Transactional
    public User registerFirstUser(AuthDtos.RegisterRequest request) {
        if (users.count() > 0) {
            throw new BusinessException("El registro inicial ya fue completado; solicita a un administrador crear el usuario");
        }
        return users.create(new UserDtos.CreateUserRequest(request.name(), request.email(), request.password(), Role.ADMIN));
    }
}
