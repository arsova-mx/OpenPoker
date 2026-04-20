package com.openpoker.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final int MIN_HS256_KEY_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expiration;

    private SecretKey key;

    @PostConstruct
    void initializeKey() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Invalid jwt.secret configuration: value must not be null or blank.");
        }

        byte[] keyBytes = resolveSecretBytes(secret.trim());
        if (keyBytes.length < MIN_HS256_KEY_BYTES) {
            throw new IllegalStateException(
                    "Invalid jwt.secret configuration: key must be at least 32 bytes for HS256. " +
                            "Provide a longer plain-text secret or a Base64-encoded secret representing at least 32 bytes.");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getKey() {
        return key;
    }

    private byte[] resolveSecretBytes(String configuredSecret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(configuredSecret);
            if (decoded.length > 0) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not valid Base64; fall back to treating the secret as plain text.
        }

        return configuredSecret.getBytes(StandardCharsets.UTF_8);
    }
    public String generateToken(String username) {
        return Jwts.builder().setSubject(username).setIssuedAt(new Date()).setExpiration(new Date(System.
                currentTimeMillis() + expiration)).signWith(getKey(), SignatureAlgorithm.HS256).compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            extractUsername(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}