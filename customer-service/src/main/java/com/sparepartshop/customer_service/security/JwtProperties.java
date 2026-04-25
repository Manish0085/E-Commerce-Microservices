package com.sparepartshop.customer_service.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.jwt")
@Getter
@Setter
@RefreshScope
public class JwtProperties {

    /** HMAC-SHA256 signing key. Must be at least 32 bytes (256 bits) of UTF-8. */
    private String secret;

    /** Token lifetime in milliseconds. */
    private long expirationMs;

    /** Value placed in the "iss" (issuer) claim. */
    private String issuer;
}
