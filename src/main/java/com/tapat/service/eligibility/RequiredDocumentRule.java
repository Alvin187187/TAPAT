package com.tapat.service.eligibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class RequiredDocumentRule implements EligibilityRule {
    @Override
    public Optional<RuleFinding> evaluate(EligibilityContext context) {
        List<String> missing = new ArrayList<>();
        for (String required : context.getProgram().getRequiredDocuments()) {
            if (!attached(context.getAttachedDocumentNames(), required)) {
                missing.add(required);
            }
        }
        if (missing.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "documents",
                "Missing documents: " + String.join(", ", missing) + "."));
    }

    private boolean attached(List<String> attached, String required) {
        String expected = required.toLowerCase(Locale.ROOT);
        for (String name : attached) {
            if (name != null && name.toLowerCase(Locale.ROOT).equals(expected)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String label() {
        return "Required documents";
    }
}
