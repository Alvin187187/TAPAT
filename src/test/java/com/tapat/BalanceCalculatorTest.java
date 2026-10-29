package com.tapat;

import com.tapat.exception.ValidationException;
import com.tapat.service.BalanceCalculator;
import com.tapat.util.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BalanceCalculatorTest {
    @Test
    void remainingIsAllocationMinusDisbursement() {
        assertEquals(new BigDecimal("150.00"), BalanceCalculator.remaining(Money.parse("200"), Money.parse("50")));
        assertEquals(new BigDecimal("0.00"), BalanceCalculator.remaining(null, null));
    }

    @Test
    void rejectsAmountsAboveTheUnpaidApprovalOrProgramBalance() {
        BalanceCalculator.checkDisbursement(Money.parse("40"), Money.parse("50"), Money.parse("100"));
        assertThrows(ValidationException.class,
                () -> BalanceCalculator.checkDisbursement(Money.parse("60"), Money.parse("50"), Money.parse("100")));
        assertThrows(ValidationException.class,
                () -> BalanceCalculator.checkDisbursement(Money.parse("80"), Money.parse("100"), Money.parse("70")));
        assertThrows(ValidationException.class,
                () -> BalanceCalculator.checkDisbursement(BigDecimal.ZERO, Money.parse("50"), Money.parse("100")));
    }
}
