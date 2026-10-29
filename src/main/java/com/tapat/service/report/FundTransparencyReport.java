package com.tapat.service.report;

import com.tapat.model.ProgramBalance;
import com.tapat.util.Money;

import java.util.ArrayList;
import java.util.List;

public class FundTransparencyReport implements Report {
    private final List<ProgramBalance> balances;

    public FundTransparencyReport(List<ProgramBalance> balances) {
        this.balances = List.copyOf(balances);
    }

    @Override
    public String getTitle() {
        return "Fund summary";
    }

    @Override
    public List<String> columns() {
        return List.of("Program", "Allocated", "Approved", "Released", "Confirmed", "Awaiting", "Remaining");
    }

    @Override
    public List<List<String>> rows() {
        List<List<String>> rows = new ArrayList<>();
        for (ProgramBalance balance : balances) {
            rows.add(List.of(
                    balance.getProgramName(),
                    Money.format(balance.getAllocated()),
                    Money.format(balance.getApprovedTotal()),
                    Money.format(balance.getDisbursed()),
                    Money.format(balance.getConfirmedTotal()),
                    Money.format(balance.getAwaitingTotal()),
                    Money.format(balance.getRemaining())));
        }
        return rows;
    }
}
