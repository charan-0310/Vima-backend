package com.vimainsurance.vimaadmin.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    @Value("${jwt.securitykey}")
    private String securitykey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refreshexpiration}")
    private long refreshexpiration;

    // private Key secretKey;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(securitykey));
    }
    // public JwtUtil(){
    //     byte[] keyBytes = Base64.getDecoder().decode(securitykey);
    //     this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    // }

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuer("VimaInsurance-AdminAPI")
                .setId(UUID.randomUUID().toString())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith( getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
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
        return Jwts.parserBuilder()
                .setSigningKey( getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey( getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}

