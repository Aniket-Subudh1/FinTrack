package com.personalfinancetracker.backend.dto;

public class TokenValidationRequest {
    private String token;

    public TokenValidationRequest() {
    }

    public TokenValidationRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}