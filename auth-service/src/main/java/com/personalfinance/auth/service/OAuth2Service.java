package com.personalfinance.auth.service;

import com.personalfinance.auth.entity.Role;
import com.personalfinance.auth.entity.User;
import com.personalfinance.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Handle OAuth2 login (Google)
     * Creates a new user if one doesn't exist, or updates existing user
     * Returns a JWT token for the user
     */
    @Transactional
    public String handleOAuth2Login(Authentication authentication) {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String provider = oauthToken.getAuthorizedClientRegistrationId(); // "google"

        User user = userRepository.findByEmail(email)
                .map(existingUser -> updateExistingOAuth2User(existingUser, provider))
                .orElseGet(() -> createNewOAuth2User(email, name, provider));

        return jwtService.generateToken(user);
    }

    /**
     * Update existing user with OAuth2 provider information
     */
    private User updateExistingOAuth2User(User existingUser, String provider) {
        existingUser.setProvider(provider);
        existingUser.setVerified(true); // OAuth2 users are automatically verified
        return userRepository.save(existingUser);
    }

    /**
     * Create a new user from OAuth2 information
     */
    private User createNewOAuth2User(String email, String name, String provider) {
        // Generate a secure random password for OAuth2 users
        // They will never use this password directly as they'll login via OAuth
        String secureRandomPassword = UUID.randomUUID().toString();

        User newUser = User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(secureRandomPassword))
                .verified(true) // OAuth2 users are automatically verified
                .provider(provider)
                .roles(Set.of(Role.USER))
                .build();

        return userRepository.save(newUser);
    }
}