package com.personalfinance.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationManagerProvider {
    private final ApplicationContext applicationContext;

    public AuthenticationManager getAuthenticationManager() {
        return applicationContext.getBean(AuthenticationManager.class);
    }
}