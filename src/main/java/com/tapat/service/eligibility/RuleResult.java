package com.tapat.service.eligibility;

public class RuleResult {
    private final String name;
    private final boolean passed;
    private final String detail;

    public RuleResult(String name, boolean passed, String detail) {
        this.name = name;
        this.passed = passed;
        this.detail = detail;
    }

    public String getName() {
        return name;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getDetail() {
        return detail;
    }
}
