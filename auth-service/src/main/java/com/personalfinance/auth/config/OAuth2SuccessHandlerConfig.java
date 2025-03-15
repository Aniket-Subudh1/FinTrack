package com.personalfinance.auth.config;

import com.personalfinance.auth.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class OAuth2SuccessHandlerConfig {

    private final OAuth2Service oAuth2Service;

    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String token = oAuth2Service.handleOAuth2Login(authentication);
            String redirectUrl = "http://localhost:4200/dashboard?token=" + token;
            response.sendRedirect(redirectUrl);
        };
    }
}