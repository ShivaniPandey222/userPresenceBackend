package com.userPresence1.userPresence1;

public class LoginResponse {
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String message;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    private String token;

    public LoginResponse(String message, String token) {
        this.message = message;
        this.token = token;
    }

    // Getters and Setters
}
