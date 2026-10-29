package com.tapat.repository;

import com.tapat.model.FundEntry;
import com.tapat.model.FundEntryType;

import java.math.BigDecimal;
import java.util.List;

public interface FundRepository {
    FundEntry insert(int programId, FundEntryType entryType, BigDecimal amount, Integer applicationId,
                     String remarks, int recordedBy);

    List<FundEntry> findAll();

    BigDecimal sum(int programId, FundEntryType entryType);

    BigDecimal sumDisbursedForApplication(int applicationId);
}
