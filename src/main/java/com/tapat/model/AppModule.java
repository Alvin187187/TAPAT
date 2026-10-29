package com.tapat.model;

public enum AppModule {
    DIRECTORY("Apply"),
    FUND_SUMMARY("Public funds"),
    MY_APPLICATIONS("My cases"),
    NOTICES("Inbox"),
    PROGRAMS("Programs"),
    REVIEW("Review cases"),
    FUNDS("Release money"),
    REPORTS("Audit trail"),
    STAFF("Staff");

    private final String label;

    AppModule(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
