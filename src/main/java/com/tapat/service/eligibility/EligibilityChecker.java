package com.tapat.service.eligibility;

import com.tapat.model.EligibilityStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EligibilityChecker {
    private final List<EligibilityRule> rules;

    public EligibilityChecker(List<EligibilityRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static EligibilityChecker standard() {
        return new EligibilityChecker(List.of(
                new DeadlineRule(),
                new IncomeLimitRule(),
                new RequiredDocumentRule(),
                new DuplicateApplicationRule()));
    }

    public EligibilityReport check(EligibilityContext context) {
        List<String> messages = new ArrayList<>();
        List<RuleResult> checks = new ArrayList<>();
        boolean duplicate = false;
        for (EligibilityRule rule : rules) {
            Optional<RuleFinding> finding = rule.evaluate(context);
            if (finding.isEmpty()) {
                checks.add(new RuleResult(rule.label(), true, "Passed"));
                continue;
            }
            messages.add(finding.get().getMessage());
            checks.add(new RuleResult(rule.label(), false, finding.get().getMessage()));
            if ("duplicate".equals(finding.get().getCode())) {
                duplicate = true;
            }
        }
        EligibilityStatus status = messages.isEmpty() ? EligibilityStatus.PASS : EligibilityStatus.FLAG;
        return new EligibilityReport(status, messages, duplicate, checks);
    }
}
