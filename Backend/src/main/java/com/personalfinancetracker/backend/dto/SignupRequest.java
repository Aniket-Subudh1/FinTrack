package com.personalfinancetracker.backend.dto;

public class SignupRequest {
    private String name;
    private String password;
    private String email;

    public SignupRequest(String name, String password, String email) {
        this.name = name;
        this.password = password;
        this.email = email;
    }

    public SignupRequest() {
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}
