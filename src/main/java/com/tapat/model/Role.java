package com.tapat.model;

public enum Role {
    APPLICANT("Applicant"),
    ADMINISTRATOR("Administrator"),
    FINANCE_OFFICER("Finance officer"),
    AUDITOR("Auditor");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Role fromStored(String value) {
        return Role.valueOf(value);
    }
}
