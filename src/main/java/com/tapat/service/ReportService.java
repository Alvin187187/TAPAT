package com.tapat.service;

import com.tapat.exception.AuthorizationException;
import com.tapat.model.User;
import com.tapat.repository.ApplicationRepository;
import com.tapat.repository.FundRepository;
import com.tapat.service.report.ApplicationSummaryReport;
import com.tapat.service.report.DisbursementLedgerReport;
import com.tapat.service.report.FundTransparencyReport;
import com.tapat.service.report.Report;

import java.util.List;

public class ReportService {
    private final FundService funds;
    private final ApplicationRepository applications;
    private final FundRepository fundEntries;

    public ReportService(FundService funds, ApplicationRepository applications, FundRepository fundEntries) {
        this.funds = funds;
        this.applications = applications;
        this.fundEntries = fundEntries;
    }

    public Report fundTransparency() {
        return new FundTransparencyReport(funds.summaries());
    }

    public List<Report> auditorReports(User actor) {
        if (actor == null || !actor.canExportFullReports()) {
            throw new AuthorizationException("You cannot export financial reports.");
        }
        return List.of(
                fundTransparency(),
                new ApplicationSummaryReport(applications.findAll()),
                new DisbursementLedgerReport(fundEntries.findAll()));
    }
}
