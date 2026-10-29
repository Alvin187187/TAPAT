package com.tapat.security;

public class StoredPassword {
    private final String hash;
    private final String salt;

    public StoredPassword(String hash, String salt) {
        this.hash = hash;
        this.salt = salt;
    }

    public String getHash() {
        return hash;
    }

    public String getSalt() {
        return salt;
    }
}
