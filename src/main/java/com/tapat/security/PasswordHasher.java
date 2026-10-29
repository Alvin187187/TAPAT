package com.tapat.security;

import com.tapat.exception.ValidationException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {
    private static final int ITERATIONS = 65536;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private final SecureRandom random = new SecureRandom();

    public StoredPassword hash(String password) {
        validate(password);
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(password, salt);
        return new StoredPassword(encode(hash), encode(salt));
    }

    public boolean verify(String password, String hash, String salt) {
        if (password == null || hash == null || salt == null) {
            return false;
        }
        byte[] expected = decode(hash);
        byte[] actual = derive(password, decode(salt));
        return MessageDigest.isEqual(expected, actual);
    }

    public void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters.");
        }
    }

    private byte[] derive(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash the password.", ex);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    private static byte[] decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
