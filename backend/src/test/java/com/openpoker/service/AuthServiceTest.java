package com.openpoker.service;

import com.openpoker.dto.AuthResponse;
import com.openpoker.dto.RegisterRequest;
import com.openpoker.entity.User;
import com.openpoker.globalexception.UserAlreadyExistsException;
import com.openpoker.repository.UserRepository;
import com.openpoker.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private final RegisterRequest request = new RegisterRequest("ana", "ana@example.com", "secret123");

    @Test
    @DisplayName("El registro devuelve un token para que el usuario quede autenticado")
    void registerReturnsToken() {
        UUID generatedId = UUID.randomUUID();
        when(userRepository.findByUsername("ana")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(generatedId);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals(generatedId, response.id());
        assertEquals("ana", response.username());
        verify(userRepository).save(argThat(user -> "hashed".equals(user.getPasswordHash())));
    }

    @Test
    @DisplayName("No permite registrar un username existente")
    void registerRejectsDuplicateUsername() {
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }
}
