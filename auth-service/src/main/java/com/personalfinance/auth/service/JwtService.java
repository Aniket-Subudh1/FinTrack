package com.personalfinance.auth.service;

import com.personalfinance.auth.config.JwtConfig;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    public String extractUsername(String token) {
        return jwtConfig.extractUsername(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return jwtConfig.extractClaim(token, claimsResolver);
    }

    public String generateToken(UserDetails userDetails) {
        return jwtConfig.generateToken(userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return jwtConfig.generateToken(extraClaims, userDetails);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return jwtConfig.generateRefreshToken(userDetails);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return jwtConfig.isTokenValid(token, userDetails);
    }
}