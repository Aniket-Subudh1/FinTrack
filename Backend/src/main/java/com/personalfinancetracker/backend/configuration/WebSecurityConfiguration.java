package com.personalfinancetracker.backend.configuration;

import com.personalfinancetracker.backend.filters.JwtRequestFilter;
import com.personalfinancetracker.backend.services.jwt.CustomerServiceImpl;
import com.personalfinancetracker.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    private final JwtRequestFilter jwtRequestFilter;
    private final CustomerServiceImpl customerService;
    private final JwtUtil jwtUtil;

    @Autowired
    public WebSecurityConfiguration(JwtRequestFilter jwtRequestFilter, CustomerServiceImpl customerService, JwtUtil jwtUtil) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.customerService = customerService;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of("http://localhost:4200"));
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/signup",
                                "/signup/verify-otp",
                                "/login",
                                "/forgot-password",
                                "/forgot-password/reset",
                                "/api/expenses",
                                "/api/customers/names"
                        ).permitAll()  // Public endpoints
                        .requestMatchers("/api/**").authenticated()  // All other API endpoints require authentication
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            // Cast authentication to OAuth2AuthenticationToken to access user details
                            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
                            String email = token.getPrincipal().getAttribute("email");
                            String name = token.getPrincipal().getAttribute("name");

                            // Save the user details in the database if not already present (use CustomerService)
                            customerService.saveOAuth2User(name, email, "dummyPassword", "Google");

                            // Generate JWT token for OAuth2 users
                            String jwtToken = jwtUtil.generateToken(email);

                            // Add the JWT token to the response header
                            response.setHeader("Authorization", "Bearer " + jwtToken);

                            // Redirect to the frontend dashboard with the token (optional)
                            response.sendRedirect("http://localhost:4200/dashboard?token=" + jwtToken);
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // Ensure stateless sessions
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);  // Add JWT filter

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
