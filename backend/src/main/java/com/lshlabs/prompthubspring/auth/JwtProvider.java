package com.lshlabs.prompthubspring.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtProvider {
    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtProvider(@Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${app.security.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String createAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(userId))
                .claims(Map.of("email", email, "type", "access", "jti", UUID.randomUUID().toString()))
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(accessExpirationMs))).signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(userId))
                .claims(Map.of("type", "refresh", "jti", UUID.randomUUID().toString())).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshExpirationMs))).signWith(key).compact();
    }

    public Long parseRefreshToken(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("invalid token type");
        }
        return Long.parseLong(claims.getSubject());
    }

    public boolean isValidRefreshToken(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }
}
