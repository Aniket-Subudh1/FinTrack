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
                                "/api/expenses/categories",
                                "/api/user/"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
                            String email = token.getPrincipal().getAttribute("email");
                            String name = token.getPrincipal().getAttribute("name");

                            customerService.saveOAuth2User(name, email, "dummyPassword", "Google");
                            String jwtToken = jwtUtil.generateToken(email);
                            response.setHeader("Authorization", "Bearer " + jwtToken);


                            response.sendRedirect("http://localhost:4200/dashboard?token=" + jwtToken);
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

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
