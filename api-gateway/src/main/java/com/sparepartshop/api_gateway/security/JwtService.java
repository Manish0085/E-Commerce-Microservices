package com.sparepartshop.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Parses and verifies incoming JWTs.
 * The gateway never ISSUES tokens — that lives in customer-service's JwtService.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties props;
    private SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        String secret = props.getJwt().getSecret();
        if (secret == null) {
            throw new IllegalStateException("security.jwt.secret is not configured");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes. Got " + keyBytes.length);
        }
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        log.info("Gateway JwtService initialized — issuer: {}", props.getJwt().getIssuer());
    }

    /**
     * Verifies the signature and expiry, returns the claims payload.
     * Throws JwtException subclasses on invalid/expired/forged tokens.
     */
    public Claims parseAndVerify(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
