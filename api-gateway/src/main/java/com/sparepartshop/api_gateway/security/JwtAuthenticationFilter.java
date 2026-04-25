package com.sparepartshop.api_gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparepartshop.api_gateway.dto.ErrorResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates incoming JWTs and enriches the forwarded request with user identity headers.
 *
 * Flow:
 *   1. If path matches any configured public path → let it through unchecked.
 *   2. Otherwise require an "Authorization: Bearer <token>" header.
 *   3. Parse + verify the token (signature, expiry, issuer). Reject 401 on failure.
 *   4. Attach X-User-Id / X-User-Role / X-User-Phone headers to the downstream request
 *      so business services can read "who's calling" without re-doing JWT work.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtService jwtService;
    private final JwtProperties props;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   JwtProperties props,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : props.getPublicPaths()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(request, response, "Missing or malformed Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        Claims claims;
        try {
            claims = jwtService.parseAndVerify(token);
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired at {} for path {}", ex.getClaims().getExpiration(), request.getRequestURI());
            writeUnauthorized(request, response, "Token has expired");
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed for {}: {}", request.getRequestURI(), ex.getMessage());
            writeUnauthorized(request, response, "Invalid token");
            return;
        }

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        String phone = claims.get("phone", String.class);

        // Populate Spring Security context — useful if we ever add route-level @PreAuthorize checks.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );

        HttpServletRequest enriched = new IdentityHeaderRequest(request, userId, role, phone);
        try {
            chain.doFilter(enriched, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeUnauthorized(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String message) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Wraps the incoming request so that X-User-* headers appear to downstream services
     * as if they'd been sent by the original client. Any client-supplied X-User-* headers
     * are overridden — we trust only what the gateway injected.
     */
    private static final class IdentityHeaderRequest extends HttpServletRequestWrapper {

        private final Map<String, String> injected = new HashMap<>();

        IdentityHeaderRequest(HttpServletRequest request, String userId, String role, String phone) {
            super(request);
            injected.put("X-User-Id", userId);
            if (role != null) injected.put("X-User-Role", role);
            if (phone != null) injected.put("X-User-Phone", phone);
        }

        @Override
        public String getHeader(String name) {
            String override = matchInjected(name);
            if (override != null) return injected.get(override);
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String override = matchInjected(name);
            if (override != null) {
                return Collections.enumeration(Collections.singletonList(injected.get(override)));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            java.util.Set<String> names = new java.util.LinkedHashSet<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String n = original.nextElement();
                if (matchInjected(n) == null) {
                    names.add(n);
                }
            }
            names.addAll(injected.keySet());
            return Collections.enumeration(names);
        }

        private String matchInjected(String name) {
            for (String key : injected.keySet()) {
                if (key.equalsIgnoreCase(name)) return key;
            }
            return null;
        }
    }
}
