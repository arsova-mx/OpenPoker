package com.openpoker.ratelimit;

import com.openpoker.globalexception.TooManyRequestsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Límite de intentos por IP en los endpoints públicos que se pueden abusar: login, registro y
 * entrada de invitados. La IP es la del cliente real: detrás del nginx del frontend, Tomcat la toma
 * de X-Forwarded-For (server.forward-headers-strategy=native).
 *
 * Se registra después del filtro de CORS para que el navegador pueda leer el 429 en peticiones cross-origin.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String GUEST_JOIN_PATTERN = "/api/sessions/*/guests";

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        String name;
        RateLimitProperties.Policy policy;
        if ("/api/auth/login".equals(path)) {
            name = "login";
            policy = properties.login();
        } else if ("/api/auth/register".equals(path)) {
            name = "register";
            policy = properties.register();
        } else if (pathMatcher.match(GUEST_JOIN_PATTERN, path)) {
            name = "guest-join";
            policy = properties.guestJoin();
        } else {
            chain.doFilter(request, response);
            return;
        }

        Optional<Duration> retryAfter = rateLimiter.tryConsume(name + ":" + request.getRemoteAddr(), policy);
        if (retryAfter.isPresent()) {
            writeTooManyRequests(response, new TooManyRequestsException(retryAfter.get()));
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeTooManyRequests(HttpServletResponse response, TooManyRequestsException ex) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // El mensaje es texto propio sin comillas; no hace falta un serializador JSON
        response.getWriter().write("{\"message\":\"" + ex.getMessage() + "\",\"retryAfterSeconds\":"
                + ex.getRetryAfterSeconds() + "}");
    }
}
