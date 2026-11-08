package com.opticaguinto.optica.users;

import com.opticaguinto.optica.auth.AuthDtos;
import com.opticaguinto.optica.auth.AuthService;
import com.opticaguinto.optica.consultations.ConsultationRepository;
import com.opticaguinto.optica.sales.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UserCrudTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthService authService;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private SaleRepository saleRepository;

    @BeforeEach
    void cleanUsers() {
        consultationRepository.deleteAll();
        saleRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String login(String email, String password) {
        return authService.login(new AuthDtos.LoginRequest(email, password)).accessToken();
    }

    @Test
    void adminUpdatesUserAndKeepsPasswordWhenOmitted() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Admin", "admin@test.local", "password-123", Role.ADMIN));
        User victim = userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-456", Role.RECEPCION));
        String admin = login("admin@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/" + victim.getId())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Recepcionista","email":"rece@test.local","role":"OPTOMETRISTA","password":null,"active":true}
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Recepcionista"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("OPTOMETRISTA"));

        // password de antes sigue funcionando
        if (login("rece@test.local", "password-456").isBlank()) {
            throw new AssertionError("password ya no funciona tras PUT sin password");
        }
    }

    @Test
    void adminSoftDeletesAndSeesItInList() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Admin", "admin@test.local", "password-123", Role.ADMIN));
        User victim = userService.create(new UserDtos.CreateUserRequest("Baja", "baja@test.local", "password-789", Role.RECEPCION));
        String admin = login("admin@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/" + victim.getId())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users")
                        .header("Authorization", "Bearer " + admin)
                        .param("q", "baja@test.local"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].active").value(false));
    }

    @Test
    void nonAdminCanListButCannotMutate() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Rece", "rece@test.local", "password-123", Role.RECEPCION));
        User other = userService.create(new UserDtos.CreateUserRequest("Otro", "otro@test.local", "password-123", Role.OPTOMETRISTA));
        String rece = login("rece@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users")
                        .header("Authorization", "Bearer " + rece)
                        .param("q", "otro"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].email").value("otro@test.local"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/" + other.getId())
                        .header("Authorization", "Bearer " + rece)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hacked","email":"otro@test.local","role":"ADMIN","password":null,"active":true}
                                """))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/" + other.getId())
                        .header("Authorization", "Bearer " + rece))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void adminCannotDeactivateOrDemoteSelf() throws Exception {
        User admin = userService.create(new UserDtos.CreateUserRequest("Admin", "admin@test.local", "password-123", Role.ADMIN));
        String token = login("admin@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/" + admin.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Admin","email":"admin@test.local","role":"RECEPCION","password":null,"active":true}
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/" + admin.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void duplicateEmailOnUpdateIsRejected() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Admin", "admin@test.local", "password-123", Role.ADMIN));
        User victim = userService.create(new UserDtos.CreateUserRequest("Vic", "vic@test.local", "password-123", Role.RECEPCION));
        String admin = login("admin@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/" + victim.getId())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Vic","email":"ADMIN@test.local","role":"RECEPCION","password":null,"active":true}
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void lowercaseRoleIsAcceptedOnCreateAndUpdate() throws Exception {
        userService.create(new UserDtos.CreateUserRequest("Admin", "admin@test.local", "password-123", Role.ADMIN));
        String admin = login("admin@test.local", "password-123");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mini","email":"mini@test.local","password":"password-123","role":"optometrista"}
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("OPTOMETRISTA"));

        String id = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users")
                        .header("Authorization", "Bearer " + admin)
                        .param("q", "mini@test.local"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        String userId = id.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mini","email":"mini@test.local","role":"admin","password":null,"active":true}
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bad","email":"bad@test.local","password":"password-123","role":"gerente"}
                                """))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
