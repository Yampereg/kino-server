package com.example.kino.auth;

import com.example.kino.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // In production store this in an environment variable or vault and ensure proper key length.
    private static final String SECRET_KEY = "your-256-bit-secret-your-256-bit-secret"; // 32+ bytes
    private static final long EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24 hours

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("name", user.getName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Validates token.
     *
     * IMPORTANT: This method will re-throw ExpiredJwtException so callers (filters) can
     * differentiate expired tokens from other invalid tokens and react accordingly.
     *
     * Returns true if token is valid and not expired; returns false for other invalid token cases.
     */
    public boolean validateToken(String token) {
        try {
            // parseClaimsJws will throw ExpiredJwtException when token is expired
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // propagate expiry so filter can handle redirect/401-expired semantics
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            // invalid signature, malformed token, unsupported token, etc.
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
