package com.openpoker.service;

import com.openpoker.dto.AuthResponse;
import com.openpoker.dto.LoginRequest;
import com.openpoker.dto.RegisterRequest;
import com.openpoker.entity.User;
import com.openpoker.globalexception.UserAlreadyExistException;
import com.openpoker.repository.UserRepository;
import com.openpoker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if(userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistException("Username");
        }
        if(userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistException("Email");
        }

        User user = User.builder().username(request.username()).email(request.email()).passwordHash(passwordEncoder
                .encode(request.password())).build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username()).orElseThrow(() -> new RuntimeException(
                "Credenciales invalidas"));

        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Credenciales invalidas");
        }

        String token = jwtService.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername());
    }
}
