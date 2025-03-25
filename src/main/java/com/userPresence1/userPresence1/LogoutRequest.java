package com.userPresence1.userPresence1;

public class LogoutRequest {
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LogoutRequest(String username) {
        this.username = username;
    }

    private String username;
    // Getters and Setters
}
