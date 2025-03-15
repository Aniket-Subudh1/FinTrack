package com.personalfinance.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final List<String> excludedUrls = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/verify-otp",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/oauth2/google"
    );

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip authentication for excluded URLs
            if (isExcludedUrl(request.getURI().getPath())) {
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            try {
                if (!isValidToken(token)) {
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }

                // Extract claims and add as headers to propagate to microservices
                Claims claims = extractClaims(token);
                String userId = claims.getSubject();
                String roles = claims.get("roles", String.class);

                // Add user information to request headers
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles != null ? roles : "")
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                return onError(exchange, "Invalid token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isExcludedUrl(String path) {
        return excludedUrls.stream().anyMatch(path::startsWith);
    }

    private boolean isValidToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    public static class Config {
        // Put configuration properties here if needed
    }
}