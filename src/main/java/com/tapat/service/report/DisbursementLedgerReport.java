package com.tapat.service.report;

import com.tapat.model.FundEntry;
import com.tapat.model.FundEntryType;
import com.tapat.util.Money;
import com.tapat.util.Text;

import java.util.ArrayList;
import java.util.List;

public class DisbursementLedgerReport implements Report {
    private final List<FundEntry> entries;

    public DisbursementLedgerReport(List<FundEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    @Override
    public String getTitle() {
        return "Disbursement ledger";
    }

    @Override
    public List<String> columns() {
        return List.of("Date", "Program", "Reference", "Applicant", "Amount", "Recorded by", "Remarks");
    }

    @Override
    public List<List<String>> rows() {
        List<List<String>> rows = new ArrayList<>();
        for (FundEntry entry : entries) {
            if (entry.getEntryType() != FundEntryType.DISBURSEMENT) {
                continue;
            }
            rows.add(List.of(
                    Text.dateTime(entry.getRecordedAt()),
                    entry.getProgramName(),
                    Text.emptyIfNull(entry.getApplicationReference()),
                    Text.emptyIfNull(entry.getApplicantName()),
                    Money.format(entry.getAmount()),
                    entry.getRecordedByName(),
                    Text.emptyIfNull(entry.getRemarks())));
        }
        return rows;
    }
}
