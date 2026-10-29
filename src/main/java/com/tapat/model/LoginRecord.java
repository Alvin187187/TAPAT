package com.tapat.model;

public class LoginRecord {
    private final User user;
    private final String passwordHash;
    private final String salt;

    public LoginRecord(User user, String passwordHash, String salt) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }
}
