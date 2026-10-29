package com.tapat.service.eligibility;

import com.tapat.util.Money;

import java.math.BigDecimal;
import java.util.Optional;

public class IncomeLimitRule implements EligibilityRule {
    @Override
    public Optional<RuleFinding> evaluate(EligibilityContext context) {
        BigDecimal limit = context.getProgram().getIncomeLimit();
        if (limit == null) {
            return Optional.empty();
        }
        BigDecimal income = context.getHouseholdIncome();
        if (income == null) {
            return Optional.of(new RuleFinding("income", "Household income was not provided."));
        }
        if (income.compareTo(limit) > 0) {
            return Optional.of(new RuleFinding(
                    "income",
                    "Household income is above the program limit of " + Money.format(limit) + "."));
        }
        return Optional.empty();
    }

    @Override
    public String label() {
        return "Income limit";
    }
}
