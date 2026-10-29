package com.tapat.service.eligibility;

import java.util.Optional;

public interface EligibilityRule {
    Optional<RuleFinding> evaluate(EligibilityContext context);

    String label();
}
