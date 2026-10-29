package com.tapat.model;

public enum FundEntryType {
    ALLOCATION("Allocation"),
    DISBURSEMENT("Disbursement");

    private final String label;

    FundEntryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
