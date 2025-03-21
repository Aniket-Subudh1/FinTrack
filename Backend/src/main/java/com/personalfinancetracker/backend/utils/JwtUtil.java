package com.personalfinancetracker.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // Default secure key - this should be 32+ characters (256+ bits)
    private static final String DEFAULT_SECRET = "fintrack_secure_jwt_secret_key_with_minimum_256_bits_length_for_maximum_security";

    @Value("${jwt.secret:" + DEFAULT_SECRET + "}")
    private String secretString;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    @Value("${jwt.cookie.name:fintrack_jwt}")
    private String jwtCookieName;

    @Value("${app.domain:localhost}")
    private String domain;

    private SecretKey secretKey;

    // Initialize the secret key properly
    private SecretKey getSecretKey() {
        if (secretKey == null) {
            try {
                if (secretString.length() < 32) {
                    logger.warn("JWT secret is less than 32 characters (256 bits). This is insecure. Using a secure random key instead.");
                    byte[] randomKeyBytes = new byte[32]; // 256 bits
                    new SecureRandom().nextBytes(randomKeyBytes);
                    secretKey = Keys.hmacShaKeyFor(randomKeyBytes);
                } else {
                    secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                logger.error("Error creating JWT secret key: {}", e.getMessage(), e);
                // Fallback to a secure random key
                byte[] randomKeyBytes = new byte[32]; // 256 bits
                new SecureRandom().nextBytes(randomKeyBytes);
                secretKey = Keys.hmacShaKeyFor(randomKeyBytes);
            }
        }
        return secretKey;
    }

    public String extractUsername(String token) {
        String username = extractClaim(token, Claims::getSubject);
        logger.debug("Extracted username from token: {}", username);
        return username;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        logger.debug("Generating token for user: {}", username);

        // Ensure we're not trying to use a numeric ID as username
        if (username != null && username.matches("\\d+")) {
            logger.warn("Attempted to generate token with numeric ID instead of email: {}", username);
        }

        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String userName) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Get JWT from Cookie
    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookieName);
        if (cookie != null) {
            logger.debug("Found JWT cookie: {}", jwtCookieName);
            return cookie.getValue();
        } else {
            logger.debug("JWT cookie not found: {}", jwtCookieName);
            return null;
        }
    }
    public void addJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(jwtCookieName, token)
                .path("/")
                .maxAge(24 * 60 * 60) // 1 day
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        logger.debug("Added JWT cookie: {}", cookie.toString());
    }
    // Create JWT Cookie
    public void createJwtCookie(HttpServletResponse response, String jwt) {
        ResponseCookie cookie = ResponseCookie.from(jwtCookieName, jwt)
                .path("/")
                .maxAge(expiration / 1000)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax") // Changed from 'Strict' to 'Lax' for better compatibility
                .domain(domain)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        logger.debug("JWT cookie created for domain: {}", domain);
    }

    public void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtCookieName, null)
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        logger.info("Cleared JWT cookie: {}", cookie.toString());
    }
}