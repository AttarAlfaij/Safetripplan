package com.safetrip.security;

import com.safetrip.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class JwtUtil {

    private static final Logger LOG = Logger.getLogger(JwtUtil.class.getName());
    private static final long DEFAULT_EXPIRATION_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret:}") String secretProperty,
                   @Value("${jwt.expiration-ms:0}") long expirationProperty) {
        
        String secret = secretProperty;
        if (secret == null || secret.isBlank()) {
            secret = System.getenv("JWT_SECRET");
        }
        if (secret == null || secret.isBlank()) {
            secret = System.getProperty("jwt.secret");
        }

        if (secret != null && !secret.isBlank()) {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 32) {
                // Pad to 256 bits if user-provided secret is short
                byte[] padded = new byte[32];
                System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
                this.signingKey = Keys.hmacShaKeyFor(padded);
            } else {
                this.signingKey = Keys.hmacShaKeyFor(keyBytes);
            }
            LOG.info("Configured JWT signing key from environment/properties.");
        } else {
            // Securely generate random 256-bit key at application startup without hardcoding secrets
            this.signingKey = Jwts.SIG.HS256.key().build();
            LOG.info("No JWT_SECRET configured; generated secure in-memory HMAC-SHA256 signing key.");
        }

        if (expirationProperty > 0) {
            this.expirationMs = expirationProperty;
        } else {
            String envExp = System.getenv("JWT_EXPIRATION_MS");
            if (envExp != null && !envExp.isBlank()) {
                long parsed = DEFAULT_EXPIRATION_MS;
                try {
                    parsed = Long.parseLong(envExp.trim());
                } catch (NumberFormatException ignored) {
                }
                this.expirationMs = parsed;
            } else {
                this.expirationMs = DEFAULT_EXPIRATION_MS;
            }
        }
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        if (user.getName() != null) {
            claims.put("name", user.getName());
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            } else if (userIdObj != null) {
                return Long.parseLong(userIdObj.toString());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration() != null && !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            LOG.warning("JWT token is expired: " + e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            LOG.warning("Invalid JWT token: " + e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
