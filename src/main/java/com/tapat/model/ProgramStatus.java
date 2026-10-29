package com.tapat.model;

public enum ProgramStatus {
    OPEN("Open"),
    CLOSED("Closed");

    private final String label;

    ProgramStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
