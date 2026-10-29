package com.tapat;

import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;
import com.tapat.model.EligibilityStatus;
import com.tapat.model.Program;
import com.tapat.model.ProgramCategory;
import com.tapat.model.ProgramStatus;
import com.tapat.model.User;
import com.tapat.service.eligibility.DeadlineRule;
import com.tapat.service.eligibility.DuplicateApplicationRule;
import com.tapat.service.eligibility.EligibilityChecker;
import com.tapat.service.eligibility.EligibilityContext;
import com.tapat.service.eligibility.EligibilityReport;
import com.tapat.service.eligibility.IncomeLimitRule;
import com.tapat.service.eligibility.RequiredDocumentRule;
import com.tapat.service.eligibility.RuleFinding;
import com.tapat.util.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EligibilityCheckerTest {
    private final LocalDate today = LocalDate.of(2026, 10, 28);
    private final EligibilityChecker checker = EligibilityChecker.standard();

    @Test
    void deadlineFlagsOnlyAfterTheDate() {
        assertTrue(new DeadlineRule().evaluate(context(program(today.minusDays(1), "25000", List.of()), "15000", List.of(), List.of())).isPresent());
        assertTrue(new DeadlineRule().evaluate(context(program(today, "25000", List.of()), "15000", List.of(), List.of())).isEmpty());
        assertTrue(new DeadlineRule().evaluate(context(program(null, "25000", List.of()), "15000", List.of(), List.of())).isEmpty());
    }

    @Test
    void incomeLimitFlagsMissingAndOverLimitAmounts() {
        IncomeLimitRule rule = new IncomeLimitRule();
        assertTrue(rule.evaluate(context(program(today, "25000", List.of()), null, List.of(), List.of())).isPresent());
        assertTrue(rule.evaluate(context(program(today, "25000", List.of()), "25000.01", List.of(), List.of())).isPresent());
        assertTrue(rule.evaluate(context(program(today, "25000", List.of()), "25000", List.of(), List.of())).isEmpty());
        assertTrue(rule.evaluate(context(program(today, null, List.of()), null, List.of(), List.of())).isEmpty());
    }

    @Test
    void requiredDocumentsFlagWhatIsMissing() {
        Optional<RuleFinding> finding = new RequiredDocumentRule().evaluate(context(
                program(today, "25000", List.of("School ID", "Enrollment form")),
                "15000",
                List.of("School ID"),
                List.of()));
        assertTrue(finding.isPresent());
        assertTrue(finding.get().getMessage().contains("Enrollment form"));
        assertTrue(new RequiredDocumentRule().evaluate(context(
                program(today, "25000", List.of("School ID")),
                "15000",
                List.of("School ID"),
                List.of())).isEmpty());
    }

    @Test
    void duplicateRuleFlagsSameApplicantOrSameName() {
        Program program = program(today, "25000", List.of());
        Application open = application(4, 9, program.getId(), "Maria Santos", ApplicationStatus.PENDING);
        Application rejected = application(5, 9, program.getId(), "Maria Santos", ApplicationStatus.REJECTED);
        Application otherProgram = application(6, 9, 99, "Maria Santos", ApplicationStatus.PENDING);
        Application sameName = application(7, 3, program.getId(), "maria   santos", ApplicationStatus.UNDER_REVIEW);

        DuplicateApplicationRule rule = new DuplicateApplicationRule();
        assertTrue(rule.evaluate(context(program, 9, "Maria Santos", "15000", List.of(), List.of(open))).isPresent());
        assertTrue(rule.evaluate(context(program, 9, "Maria Santos", "15000", List.of(), List.of(rejected, otherProgram))).isEmpty());
        assertTrue(rule.evaluate(context(program, 10, "Maria Santos", "15000", List.of(), List.of(sameName))).isPresent());
        assertEquals("duplicate", rule.evaluate(context(program, 10, "Maria Santos", "15000", List.of(), List.of(sameName))).get().getCode());
    }

    @Test
    void checkerCombinesFlagsAndDoesNotApprove() {
        Program program = program(today.minusDays(2), "10000", List.of("Valid ID"));
        Application existing = application(2, 9, program.getId(), "Maria Santos", ApplicationStatus.PENDING);
        EligibilityReport report = checker.check(context(program, 9, "Maria Santos", "18000", List.of(), List.of(existing)));
        assertEquals(EligibilityStatus.FLAG, report.getStatus());
        assertTrue(report.isDuplicate());
        assertTrue(report.getMessages().size() >= 3);
        assertFalse(report.notes().isBlank());
    }

    private EligibilityContext context(Program program, String income, List<String> documents, List<Application> existing) {
        return context(program, 9, "Maria Santos", income, documents, existing);
    }

    private EligibilityContext context(Program program, int applicantId, String name, String income,
                                      List<String> documents, List<Application> existing) {
        return new EligibilityContext(program, applicantId, name, income == null ? null : Money.parse(income),
                documents, existing, 0, today);
    }

    private Program program(LocalDate deadline, String incomeLimit, List<String> documents) {
        return new Program(8, "Tertiary scholarship", ProgramCategory.SCHOLARSHIP, "Tuition help",
                "Enrolled students", documents, "Keep the reference number.", deadline,
                new BigDecimal("20000.00"), incomeLimit == null ? null : Money.parse(incomeLimit),
                ProgramStatus.OPEN, 1, LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private Application application(int id, int applicantId, int programId, String name, ApplicationStatus status) {
        LocalDateTime submitted = LocalDateTime.of(2026, 10, 1, 9, 0);
        return new Application(id, "TAPAT-2026-0000" + id, applicantId, name, programId, "Tertiary scholarship",
                status, Money.parse("15000"), "Tuition", Money.parse("1000"), null, EligibilityStatus.PASS, "",
                false, null, null, submitted, submitted, null, null, null);
    }
}
