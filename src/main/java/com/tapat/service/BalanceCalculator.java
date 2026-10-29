package com.tapat.service;

import com.tapat.exception.ValidationException;
import com.tapat.util.Money;

import java.math.BigDecimal;

public class BalanceCalculator {
    private BalanceCalculator() {
    }

    public static BigDecimal remaining(BigDecimal allocated, BigDecimal disbursed) {
        return Money.zeroIfNull(allocated).subtract(Money.zeroIfNull(disbursed));
    }

    public static void checkDisbursement(BigDecimal amount, BigDecimal unpaidApproved, BigDecimal programRemaining) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Enter an amount greater than zero.");
        }
        if (amount.compareTo(Money.zeroIfNull(unpaidApproved)) > 0) {
            throw new ValidationException("Amount exceeds the approved assistance still unpaid.");
        }
        if (amount.compareTo(Money.zeroIfNull(programRemaining)) > 0) {
            throw new ValidationException("Amount exceeds the program balance.");
        }
    }
}
