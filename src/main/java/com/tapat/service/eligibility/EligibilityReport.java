package com.tapat.service.eligibility;

import com.tapat.model.EligibilityStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EligibilityReport {
    private final EligibilityStatus status;
    private final List<String> messages;
    private final boolean duplicate;
    private final List<RuleResult> checks;

    public EligibilityReport(EligibilityStatus status, List<String> messages, boolean duplicate) {
        this(status, messages, duplicate, List.of());
    }

    public EligibilityReport(EligibilityStatus status, List<String> messages, boolean duplicate, List<RuleResult> checks) {
        this.status = status;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.duplicate = duplicate;
        this.checks = Collections.unmodifiableList(new ArrayList<>(checks));
    }

    public EligibilityStatus getStatus() {
        return status;
    }

    public List<String> getMessages() {
        return messages;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public List<RuleResult> getChecks() {
        return checks;
    }

    public String notes() {
        return String.join("\n", messages);
    }
}
