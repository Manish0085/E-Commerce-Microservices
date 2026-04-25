package com.sparepartshop.api_gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "security")
public class JwtProperties {

    private Jwt jwt = new Jwt();
    private List<String> publicPaths = new ArrayList<>();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }

    public List<String> getPublicPaths() { return publicPaths; }
    public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }

    public static class Jwt {
        private String secret;
        private String issuer;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
    }
}
