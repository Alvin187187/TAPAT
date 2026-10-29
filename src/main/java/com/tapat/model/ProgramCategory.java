package com.tapat.model;

public enum ProgramCategory {
    SCHOLARSHIP("Scholarship"),
    MEDICAL("Medical assistance"),
    EMERGENCY("Emergency aid"),
    LIVELIHOOD("Livelihood support");

    private final String label;

    ProgramCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
