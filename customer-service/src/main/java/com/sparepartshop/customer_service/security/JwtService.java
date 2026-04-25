package com.sparepartshop.customer_service.security;

import com.sparepartshop.customer_service.entity.Customer;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Responsible for issuing JWTs on successful login.
 * Token validation lives in the gateway — this service never parses incoming tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties props;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes (256 bits). Got " + keyBytes.length);
        }
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        log.info("JwtService initialized — token lifetime: {} ms, issuer: {}",
                props.getExpirationMs(), props.getIssuer());
    }

    public String generateToken(Customer customer) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(props.getExpirationMs());

        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(customer.getId().toString())
                .claim("role", customer.getRole().name())
                .claim("phone", customer.getPhone())
                .claim("name", customer.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public long getExpirationMs() {
        return props.getExpirationMs();
    }
}
