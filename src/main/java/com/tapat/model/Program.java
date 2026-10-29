package com.tapat.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Program {
    private final int id;
    private final String name;
    private final ProgramCategory category;
    private final String description;
    private final String eligibilitySummary;
    private final List<String> requiredDocuments;
    private final String instructions;
    private final LocalDate deadline;
    private final BigDecimal maxAssistance;
    private final BigDecimal incomeLimit;
    private final ProgramStatus status;
    private final Integer createdBy;
    private final LocalDateTime createdAt;

    public Program(int id, String name, ProgramCategory category, String description, String eligibilitySummary,
                   List<String> requiredDocuments, String instructions, LocalDate deadline, BigDecimal maxAssistance,
                   BigDecimal incomeLimit, ProgramStatus status, Integer createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.eligibilitySummary = eligibilitySummary;
        this.requiredDocuments = Collections.unmodifiableList(new ArrayList<>(requiredDocuments));
        this.instructions = instructions;
        this.deadline = deadline;
        this.maxAssistance = maxAssistance;
        this.incomeLimit = incomeLimit;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ProgramCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getEligibilitySummary() {
        return eligibilitySummary;
    }

    public List<String> getRequiredDocuments() {
        return requiredDocuments;
    }

    public String getInstructions() {
        return instructions;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public BigDecimal getMaxAssistance() {
        return maxAssistance;
    }

    public BigDecimal getIncomeLimit() {
        return incomeLimit;
    }

    public ProgramStatus getStatus() {
        return status;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
