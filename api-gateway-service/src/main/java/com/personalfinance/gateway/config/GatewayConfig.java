package com.personalfinance.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .uri("lb://AUTH-SERVICE"))
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .uri("lb://USER-SERVICE"))
                .route("expense-service", r -> r
                        .path("/api/expenses/**")
                        .uri("lb://EXPENSE-SERVICE"))
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .uri("lb://NOTIFICATION-SERVICE"))
                .build();
    }
}