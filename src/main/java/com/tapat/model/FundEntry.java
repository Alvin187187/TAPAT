package com.tapat.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FundEntry {
    private final int id;
    private final int programId;
    private final String programName;
    private final FundEntryType entryType;
    private final BigDecimal amount;
    private final Integer applicationId;
    private final String applicationReference;
    private final String applicantName;
    private final String remarks;
    private final int recordedBy;
    private final String recordedByName;
    private final LocalDateTime recordedAt;

    public FundEntry(int id, int programId, String programName, FundEntryType entryType, BigDecimal amount,
                     Integer applicationId, String applicationReference, String applicantName, String remarks,
                     int recordedBy, String recordedByName, LocalDateTime recordedAt) {
        this.id = id;
        this.programId = programId;
        this.programName = programName;
        this.entryType = entryType;
        this.amount = amount;
        this.applicationId = applicationId;
        this.applicationReference = applicationReference;
        this.applicantName = applicantName;
        this.remarks = remarks;
        this.recordedBy = recordedBy;
        this.recordedByName = recordedByName;
        this.recordedAt = recordedAt;
    }

    public int getId() {
        return id;
    }

    public int getProgramId() {
        return programId;
    }

    public String getProgramName() {
        return programName;
    }

    public FundEntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getRemarks() {
        return remarks;
    }

    public int getRecordedBy() {
        return recordedBy;
    }

    public String getRecordedByName() {
        return recordedByName;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
