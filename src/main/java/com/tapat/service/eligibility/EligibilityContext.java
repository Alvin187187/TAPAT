package com.tapat.service.eligibility;

import com.tapat.model.Application;
import com.tapat.model.Program;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EligibilityContext {
    private final Program program;
    private final int applicantId;
    private final String applicantName;
    private final BigDecimal householdIncome;
    private final List<String> attachedDocumentNames;
    private final List<Application> existingApplications;
    private final int currentApplicationId;
    private final LocalDate today;

    public EligibilityContext(Program program, int applicantId, String applicantName, BigDecimal householdIncome,
                              List<String> attachedDocumentNames, List<Application> existingApplications,
                              int currentApplicationId, LocalDate today) {
        this.program = program;
        this.applicantId = applicantId;
        this.applicantName = applicantName;
        this.householdIncome = householdIncome;
        this.attachedDocumentNames = attachedDocumentNames;
        this.existingApplications = existingApplications;
        this.currentApplicationId = currentApplicationId;
        this.today = today;
    }

    public Program getProgram() {
        return program;
    }

    public int getApplicantId() {
        return applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public BigDecimal getHouseholdIncome() {
        return householdIncome;
    }

    public List<String> getAttachedDocumentNames() {
        return attachedDocumentNames;
    }

    public List<Application> getExistingApplications() {
        return existingApplications;
    }

    public int getCurrentApplicationId() {
        return currentApplicationId;
    }

    public LocalDate getToday() {
        return today;
    }
}
