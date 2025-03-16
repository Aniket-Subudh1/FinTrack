package com.personalfinancetracker.backend.filters;

import com.personalfinancetracker.backend.services.auth.AuthenticationService;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final JwtUtil jwtUtil;
    private final AuthenticationService authenticationService;

    private static final List<String> EXCLUDE_URLS = List.of(
            "/signup",
            "/signup/verify-otp",
            "/signup/resend-otp",
            "/login",
            "/forgot-password",
            "/forgot-password/reset",
            "/forgot-password/resend-otp",
            "/oauth2/validate",
            "/oauth2/authorization/google",
            "/oauth2/callback",
            "/logout"
    );

    @Autowired
    public JwtRequestFilter(JwtUtil jwtUtil, @Lazy AuthenticationService authenticationService) {
        this.jwtUtil = jwtUtil;
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // Check if the path should be excluded from authentication
        if (shouldExclude(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;

        // Try to get token from cookie first
        token = jwtUtil.getJwtFromCookies(request);

        // If not in cookie, try from Authorization header (backward compatibility)
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        String username = null;

        // Extract username from token
        if (token != null) {
            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                logger.error("JWT Token extraction error: {}", e.getMessage());
            }
        }

        // Validate token and set authentication if valid
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = authenticationService.loadUserByUsername(username);

                if (jwtUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    logger.debug("Authenticated user: {}", username);
                }
            } catch (Exception e) {
                logger.error("Authentication error: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldExclude(String requestPath) {
        for (String excludeUrl : EXCLUDE_URLS) {
            if (requestPath.startsWith(excludeUrl)) {
                return true;
            }
        }
        return false;
    }
}