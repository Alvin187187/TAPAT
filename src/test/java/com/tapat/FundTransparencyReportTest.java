package com.tapat;

import com.tapat.model.ProgramBalance;
import com.tapat.service.report.FundTransparencyReport;
import com.tapat.util.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FundTransparencyReportTest {
    @Test
    void publicReportKeepsProgramTotalsOnly() {
        FundTransparencyReport report = new FundTransparencyReport(List.of(
                new ProgramBalance(1, "Tertiary scholarship", Money.parse("500000"), Money.parse("10000"), 1,
                        Money.parse("10000"), Money.parse("10000"), Money.ZERO)));
        assertEquals(List.of("Program", "Allocated", "Approved", "Released", "Confirmed", "Awaiting", "Remaining"), report.columns());
        assertFalse(report.columns().contains("Applicant"));
        assertEquals("Tertiary scholarship", report.rows().get(0).get(0));
        assertTrue(report.toCsv().contains("Tertiary scholarship"));
        assertFalse(report.toCsv().contains("Maria Santos"));
        assertTrue(report.chartSlices().isEmpty());
    }
}
