package com.tapat.model;

public enum ApplicationStatus {
    PENDING("Pending"),
    UNDER_REVIEW("Under review"),
    APPROVED("Approved"),
    FOR_RELEASE("For release"),
    RELEASED("Released"),
    VERIFIED("Verified"),
    REJECTED("Rejected");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean commitsFunds() {
        return this == APPROVED || this == FOR_RELEASE || this == RELEASED || this == VERIFIED;
    }

    public boolean isDecided() {
        return this == REJECTED || commitsFunds();
    }
}
