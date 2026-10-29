package com.tapat.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Application {
    private final int id;
    private final String referenceNo;
    private final int applicantId;
    private final String applicantName;
    private final int programId;
    private final String programName;
    private final ApplicationStatus status;
    private final BigDecimal householdIncome;
    private final String purpose;
    private final BigDecimal requestedAmount;
    private final BigDecimal approvedAmount;
    private final EligibilityStatus eligibilityStatus;
    private final String eligibilityNotes;
    private final boolean duplicateFlag;
    private final Integer reviewerId;
    private final String reviewNotes;
    private final LocalDateTime submittedAt;
    private final LocalDateTime updatedAt;
    private final String claimReference;
    private final LocalDateTime confirmedAt;
    private final String confirmedByName;

    public Application(int id, String referenceNo, int applicantId, String applicantName, int programId,
                       String programName, ApplicationStatus status, BigDecimal householdIncome, String purpose,
                       BigDecimal requestedAmount, BigDecimal approvedAmount, EligibilityStatus eligibilityStatus,
                       String eligibilityNotes, boolean duplicateFlag, Integer reviewerId, String reviewNotes,
                       LocalDateTime submittedAt, LocalDateTime updatedAt, String claimReference,
                       LocalDateTime confirmedAt, String confirmedByName) {
        this.id = id;
        this.referenceNo = referenceNo;
        this.applicantId = applicantId;
        this.applicantName = applicantName;
        this.programId = programId;
        this.programName = programName;
        this.status = status;
        this.householdIncome = householdIncome;
        this.purpose = purpose;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.eligibilityStatus = eligibilityStatus;
        this.eligibilityNotes = eligibilityNotes;
        this.duplicateFlag = duplicateFlag;
        this.reviewerId = reviewerId;
        this.reviewNotes = reviewNotes;
        this.submittedAt = submittedAt;
        this.updatedAt = updatedAt;
        this.claimReference = claimReference;
        this.confirmedAt = confirmedAt;
        this.confirmedByName = confirmedByName;
    }

    public int getId() {
        return id;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public int getApplicantId() {
        return applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public int getProgramId() {
        return programId;
    }

    public String getProgramName() {
        return programName;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public BigDecimal getHouseholdIncome() {
        return householdIncome;
    }

    public String getPurpose() {
        return purpose;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public EligibilityStatus getEligibilityStatus() {
        return eligibilityStatus;
    }

    public String getEligibilityNotes() {
        return eligibilityNotes;
    }

    public boolean isDuplicateFlag() {
        return duplicateFlag;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getClaimReference() {
        return claimReference;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public String getConfirmedByName() {
        return confirmedByName;
    }
}
