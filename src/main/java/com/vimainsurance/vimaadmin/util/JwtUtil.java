package com.vimainsurance.vimaadmin.util;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    @Value("${jwt.securitykey}")
    private String securitykey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refreshexpiration}")
    private long refreshexpiration;

    // private Key secretKey;
    
    // ✅ FIX: Cache JwtParser to prevent per-request creation
    // This reduces memory overhead from creating new parser instances on every JWT validation
    private volatile JwtParser jwtParser;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(securitykey));
    }
    
    /**
     * Get or create cached JwtParser instance
     * Uses double-checked locking pattern for thread-safe lazy initialization
     */
    private JwtParser getJwtParser() {
        if (jwtParser == null) {
            synchronized (this) {
                if (jwtParser == null) {
                    jwtParser = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build();
                }
            }
        }
        return jwtParser;
    }
    // public JwtUtil(){
    //     byte[] keyBytes = Base64.getDecoder().decode(securitykey);
    //     this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    // }

    public String generateToken(String username, String role, String email, String agentId, String organizationId) {
        String token =  Jwts.builder()
                .setSubject(username)
                .setIssuer("VimaInsurance-AdminAPI")
                .setId(UUID.randomUUID().toString())
                .claim("role", role)
                .claim("email", email)
                .claim("agentId", agentId)
                .claim("organizationId", organizationId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith( getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        return token;
    }

    public String generateRefreshToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshexpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    public String extractUsername(String token) {
        return getJwtParser()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getJwtParser().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Claims extractAllClaims(String token) {
        return getJwtParser()
                .parseClaimsJws(token)
                .getBody();
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

}

