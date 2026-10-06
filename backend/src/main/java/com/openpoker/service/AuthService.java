package com.openpoker.service;

import com.openpoker.dto.AuthResponse;
import com.openpoker.dto.LoginRequest;
import com.openpoker.dto.RegisterRequest;
import com.openpoker.entity.User;
import com.openpoker.entity.UserRole;
import com.openpoker.globalexception.InvalidCredentialsException;
import com.openpoker.globalexception.InvalidValueException;
import com.openpoker.globalexception.UserAlreadyExistsException;
import com.openpoker.ratelimit.LoginAttemptService;
import com.openpoker.repository.UserRepository;
import com.openpoker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    /**
     * Nombres que no puede tomar un usuario. "anonymousUser" es el principal que Spring usa para
     * peticiones sin autenticar; como username permitiría hacerse pasar por ellas.
     */
    static final Set<String> RESERVED_USERNAMES = Set.of(
            "anonymoususer", "admin", "administrator", "root", "system", "openpoker",
            "guest", "invitado", "host", "null", "undefined");

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    /**
     * Registra al usuario y devuelve un token, igual que el login, para que quede autenticado al instante.
     */
    public AuthResponse register(RegisterRequest request) {
        if (RESERVED_USERNAMES.contains(request.username().toLowerCase(Locale.ROOT))) {
            throw new InvalidValueException("Ese nombre de usuario no está disponible");
        }
        // Sin distinguir mayúsculas: "Ana" y "ana" se verían iguales en una sala
        if(userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new UserAlreadyExistsException("Username");
        }
        if(userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("Email");
        }

        User user = User.builder().username(request.username()).email(request.email()).passwordHash(passwordEncoder.encode(request.password())).role(UserRole.VOTER).build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(token, savedUser.getId(), savedUser.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        // Bloqueo temporal tras varios intentos fallidos contra la misma cuenta, desde cualquier IP
        loginAttemptService.ensureNotLocked(request.username());

        User user = userRepository.findByUsername(request.username()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(request.username());
            throw new InvalidCredentialsException();
        }

        loginAttemptService.recordSuccess(request.username());
        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user.getId(), user.getUsername());
    }
}
