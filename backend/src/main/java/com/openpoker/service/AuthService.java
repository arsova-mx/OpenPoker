package com.openpoker.service;

import com.openpoker.dto.AuthResponse;
import com.openpoker.dto.LoginRequest;
import com.openpoker.dto.RegisterRequest;
import com.openpoker.entity.User;
import com.openpoker.globalexception.InvalidCredentialsException;
import com.openpoker.globalexception.UserAlreadyExistsException;
import com.openpoker.repository.UserRepository;
import com.openpoker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if(userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username");
        }
        if(userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("Email");
        }

        User user = User.builder().username(request.username()).email(request.email()).passwordHash(passwordEncoder
                .encode(request.password())).build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username()).orElseThrow(() -> new
                InvalidCredentialsException());

        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername());
    }
}
