package com.openpoker.service;

import com.openpoker.dto.AuthResponse;
import com.openpoker.dto.LoginRequest;
import com.openpoker.dto.RegisterRequest;
import com.openpoker.entity.User;
import com.openpoker.entity.UserRole;
import com.openpoker.repository.UserRepository;
import com.openpoker.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerUser() {
        RegisterRequest req = new RegisterRequest("user", "test@test.com", "1234");

        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("encoded-password");
        when(jwtService.generateToken("user")).thenReturn("token-123");

        AuthResponse res = authService.register(req);

        assertNotNull(res.token());
    }

    @Test
    void loginUser() {
        User user = User.builder().username("user").role(UserRole.HOST).passwordHash("encoded-password").build();

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("user")).thenReturn("token-123");

        AuthResponse res = authService.login(new LoginRequest("user", "1234"));

        assertNotNull(res.token());
    }
}
