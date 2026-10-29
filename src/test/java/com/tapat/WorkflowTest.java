package com.tapat;

import com.tapat.exception.AuthorizationException;
import com.tapat.exception.ValidationException;
import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;
import com.tapat.model.EligibilityStatus;
import com.tapat.model.Program;
import com.tapat.model.User;
import com.tapat.service.report.Report;
import com.tapat.util.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowTest {
    @TempDir
    Path dataDir;

    @Test
    void eachRoleCanCompleteItsPartOfTheAssistanceFlow() {
        AppContext context = AppContext.open(dataDir);
        try {
            String publicCsv = context.reports().fundTransparency().toCsv();
            assertTrue(publicCsv.contains("Tertiary scholarship"));
            assertFalse(publicCsv.contains("Maria Santos"));
            assertFalse(context.programs().listOpen().isEmpty());

            User applicant = context.auth().login("applicant", "applicant123");
            Program medical = context.programs().listOpen().stream()
                    .filter(program -> program.getName().equals("Medical assistance"))
                    .findFirst()
                    .orElseThrow();
            Application submitted = context.applications().submit(
                    applicant,
                    medical.getId(),
                    Money.parse("12000"),
                    Money.parse("4000"),
                    "Medicine for a follow-up visit.",
                    Map.of());
            assertEquals(ApplicationStatus.PENDING, submitted.getStatus());
            assertEquals(EligibilityStatus.FLAG, submitted.getEligibilityStatus());
            assertTrue(submitted.isDuplicateFlag());
            assertTrue(submitted.getEligibilityNotes().contains("Missing documents"));

            User admin = context.auth().login("admin", "admin123");
            context.review().approve(admin, submitted.getId(), Money.parse("3000"), "Partial assistance approved.");
            Application approved = context.applications().require(submitted.getId());
            assertEquals(ApplicationStatus.FOR_RELEASE, approved.getStatus());
            assertThrows(AuthorizationException.class,
                    () -> context.review().approve(applicant, submitted.getId(), Money.parse("1000"), "No"));

            User finance = context.auth().login("finance", "finance123");
            int ledgerBefore = context.funds().ledger(finance).size();
            assertThrows(ValidationException.class,
                    () -> context.funds().disburse(finance, submitted.getId(), Money.parse("3001"), "Too much"));
            assertEquals(ledgerBefore, context.funds().ledger(finance).size());
            context.funds().disburse(finance, submitted.getId(), Money.parse("3000"), "Released to the applicant");
            Application released = context.applications().require(submitted.getId());
            assertEquals(ApplicationStatus.RELEASED, released.getStatus());
            assertTrue(released.getClaimReference().startsWith("TPT-R-"));
            context.funds().verifyClaim(finance, released.getClaimReference());
            assertEquals(ApplicationStatus.VERIFIED, context.applications().require(submitted.getId()).getStatus());
            assertThrows(AuthorizationException.class,
                    () -> context.funds().disburse(applicant, submitted.getId(), Money.parse("1"), "No"));

            User auditor = context.auth().login("auditor", "auditor123");
            String joined = "";
            boolean sawApplicant = false;
            boolean sawDisbursement = false;
            for (Report report : context.reports().auditorReports(auditor)) {
                String csv = report.toCsv();
                joined = joined + "\n" + csv;
                if (csv.contains("Maria Santos") && report.getTitle().equals("Application summary")) {
                    sawApplicant = true;
                }
                if (report.getTitle().equals("Disbursement ledger") && csv.contains(submitted.getReferenceNo())) {
                    sawDisbursement = true;
                }
            }
            assertTrue(sawApplicant);
            assertTrue(sawDisbursement);
            assertTrue(joined.contains("Maria Santos"));
            assertFalse(context.reports().fundTransparency().toCsv().contains("Maria Santos"));
            assertThrows(AuthorizationException.class, () -> context.reports().auditorReports(applicant));
        } finally {
            context.close();
        }
    }
}
