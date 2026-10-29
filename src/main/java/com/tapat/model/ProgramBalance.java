package com.tapat.model;

import java.math.BigDecimal;

public class ProgramBalance {
    private final int programId;
    private final String programName;
    private final BigDecimal allocated;
    private final BigDecimal disbursed;
    private final int approvedCount;
    private final BigDecimal approvedTotal;
    private final BigDecimal confirmedTotal;
    private final BigDecimal awaitingTotal;

    public ProgramBalance(int programId, String programName, BigDecimal allocated, BigDecimal disbursed,
                          int approvedCount, BigDecimal approvedTotal, BigDecimal confirmedTotal, BigDecimal awaitingTotal) {
        this.programId = programId;
        this.programName = programName;
        this.allocated = allocated;
        this.disbursed = disbursed;
        this.approvedCount = approvedCount;
        this.approvedTotal = approvedTotal;
        this.confirmedTotal = confirmedTotal;
        this.awaitingTotal = awaitingTotal;
    }

    public int getProgramId() {
        return programId;
    }

    public String getProgramName() {
        return programName;
    }

    public BigDecimal getAllocated() {
        return allocated;
    }

    public BigDecimal getDisbursed() {
        return disbursed;
    }

    public BigDecimal getRemaining() {
        return allocated.subtract(disbursed);
    }

    public int getApprovedCount() {
        return approvedCount;
    }

    public BigDecimal getApprovedTotal() {
        return approvedTotal;
    }

    public BigDecimal getConfirmedTotal() {
        return confirmedTotal;
    }

    public BigDecimal getAwaitingTotal() {
        return awaitingTotal;
    }

    public BigDecimal getUnreserved() {
        return allocated.subtract(approvedTotal);
    }
}
