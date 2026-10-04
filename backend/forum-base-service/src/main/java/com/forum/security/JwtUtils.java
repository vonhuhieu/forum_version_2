package com.forum.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expirationMs}")
    private int jwtExpirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // 30 ngày = 30 * 24 * 60 * 60 * 1000 = 2,592,000,000L ms
    private static final long REMEMBER_ME_EXPIRATION_MS = 30L * 24 * 60 * 60 * 1000;

    public String generateJwtToken(String username, java.util.Set<String> roles) {
        return generateJwtToken(username, roles, false);
    }

    public String generateJwtToken(String username, java.util.Set<String> roles, boolean rememberMe) {
        long duration = rememberMe ? REMEMBER_ME_EXPIRATION_MS : jwtExpirationMs;
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + duration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getRolesFromJwtToken(String token) {
        return (java.util.List<String>) Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("roles");
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("JWT Validation Error: " + e.getMessage());
        }
        return false;
    }
}
