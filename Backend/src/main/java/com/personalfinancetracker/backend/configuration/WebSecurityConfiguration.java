package com.personalfinancetracker.backend.configuration;

import com.personalfinancetracker.backend.filters.JwtRequestFilter;
import com.personalfinancetracker.backend.services.AuthService;
import com.personalfinancetracker.backend.services.auth.AuthenticationService;
import com.personalfinancetracker.backend.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfiguration.class);

    private final JwtRequestFilter jwtRequestFilter;
    private final AuthService authService;
    private final AuthenticationService authenticationService;

    @Autowired
    public WebSecurityConfiguration(
            JwtRequestFilter jwtRequestFilter,
            @Lazy AuthService authService,
            @Lazy AuthenticationService authenticationService) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.authService = authService;
        this.authenticationService = authenticationService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(authenticationService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                // Enable CSRF protection with cookie-based tokens, but disable it for API endpoints
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        // Disable CSRF for these API endpoints
                        .ignoringRequestMatchers(
                                "/signup/**",
                                "/login",
                                "/forgot-password/**",
                                "/oauth2/**",
                                "/api/**" // Temporarily disable for all API endpoints
                        )
                )
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of("http://localhost:4200"));
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
                    corsConfig.setAllowCredentials(true);
                    corsConfig.setMaxAge(3600L);
                    return corsConfig;
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/signup/**",
                                "/login",
                                "/logout",
                                "/forgot-password/**",
                                "/oauth2/**"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            try {
                                OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
                                String email = token.getPrincipal().getAttribute("email");
                                String name = token.getPrincipal().getAttribute("name");

                                if (email == null) {
                                    logger.error("OAuth2 authentication failed: email attribute is missing");
                                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email attribute is missing");
                                    return;
                                }

                                // Complete OAuth2 registration flow
                                authService.completeOAuth2Registration(name, email, "Google", response);

                                // Redirect to frontend
                                response.sendRedirect("http://localhost:4200/dashboard");
                            } catch (Exception e) {
                                logger.error("OAuth2 authentication error: {}", e.getMessage(), e);
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth2 authentication failed");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            logger.error("OAuth2 authentication failure: {}", exception.getMessage(), exception);
                            response.sendRedirect("http://localhost:4200/login?error=oauth2");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            JwtUtil jwtUtil = null;
                            jwtUtil.clearJwtCookie(response);
                            response.setStatus(HttpServletResponse.SC_OK);

                            // Return a JSON response
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Logout successful\"}");
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("fintrack_jwt") // Also clear cookie by name
                        .permitAll());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}