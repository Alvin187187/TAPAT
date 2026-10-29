package com.tapat;

import com.tapat.security.PasswordHasher;
import com.tapat.security.StoredPassword;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    void hashVerifiesTheOriginalPasswordOnly() {
        PasswordHasher hasher = new PasswordHasher();
        StoredPassword stored = hasher.hash("applicant123");
        assertTrue(hasher.verify("applicant123", stored.getHash(), stored.getSalt()));
        assertFalse(hasher.verify("applicant124", stored.getHash(), stored.getSalt()));
        assertFalse(stored.getHash().contains("applicant123"));
    }
}
