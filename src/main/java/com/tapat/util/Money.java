package com.tapat.util;

import com.tapat.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? ZERO : normalize(amount);
    }

    public static String store(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return normalize(amount).toPlainString();
    }

    public static BigDecimal load(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        return normalize(new BigDecimal(stored.trim()));
    }

    public static BigDecimal parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.trim().replace(",", "").replace("PHP", "").replace("₱", "").trim();
        try {
            return normalize(new BigDecimal(cleaned));
        } catch (NumberFormatException ex) {
            throw new ValidationException("Enter a valid amount.");
        }
    }

    public static BigDecimal requirePositive(String text, String label) {
        BigDecimal amount = parse(text);
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Enter a " + label + " greater than zero.");
        }
        return amount;
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return "PHP " + compact(amount);
    }

    public static String compact(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return new DecimalFormat("#,##0.00").format(normalize(amount));
    }
}
