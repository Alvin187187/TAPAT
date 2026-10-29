package com.tapat.service.eligibility;

import java.util.Optional;

public class DeadlineRule implements EligibilityRule {
    @Override
    public Optional<RuleFinding> evaluate(EligibilityContext context) {
        if (context.getProgram().getDeadline() == null) {
            return Optional.empty();
        }
        if (context.getProgram().getDeadline().isBefore(context.getToday())) {
            return Optional.of(new RuleFinding("deadline", "The application deadline has passed."));
        }
        return Optional.empty();
    }

    @Override
    public String label() {
        return "Deadline";
    }
}
