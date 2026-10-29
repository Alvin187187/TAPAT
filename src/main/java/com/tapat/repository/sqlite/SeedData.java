package com.tapat.repository.sqlite;

import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;
import com.tapat.model.FundEntryType;
import com.tapat.model.Program;
import com.tapat.model.ProgramCategory;
import com.tapat.model.ProgramStatus;
import com.tapat.model.Role;
import com.tapat.model.User;
import com.tapat.repository.ApplicationRepository;
import com.tapat.repository.AuditRepository;
import com.tapat.repository.DocumentRepository;
import com.tapat.repository.FundRepository;
import com.tapat.repository.NotificationRepository;
import com.tapat.repository.ProgramRepository;
import com.tapat.repository.UserRepository;
import com.tapat.security.PasswordHasher;
import com.tapat.security.StoredPassword;
import com.tapat.service.DocumentStore;
import com.tapat.service.eligibility.EligibilityChecker;
import com.tapat.service.eligibility.EligibilityContext;
import com.tapat.service.eligibility.EligibilityReport;
import com.tapat.util.Money;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SeedData {
    private SeedData() {
    }

    public static void seedIfEmpty(UserRepository users, ProgramRepository programs, ApplicationRepository applications,
                                   DocumentRepository documents, FundRepository funds, NotificationRepository notifications,
                                   AuditRepository audit, DocumentStore documentStore, PasswordHasher hasher) {
        if (users.count() > 0) {
            return;
        }
        User applicant = user(users, hasher, "applicant", "applicant123", "Maria Santos",
                "maria.santos@example.com", "09171234567", "12 Rizal Street, Quezon City", Role.APPLICANT);
        User admin = user(users, hasher, "admin", "admin123", "Ana Reyes",
                "ana.reyes@example.com", "09181234567", "TAPAT office", Role.ADMINISTRATOR);
        User finance = user(users, hasher, "finance", "finance123", "Luis Garcia",
                "luis.garcia@example.com", "09191234567", "TAPAT office", Role.FINANCE_OFFICER);
        user(users, hasher, "auditor", "auditor123", "Elena Cruz",
                "elena.cruz@example.com", "09201234567", "TAPAT office", Role.AUDITOR);

        LocalDate today = LocalDate.now();
        Program scholarship = programs.insert(program(admin, "Tertiary scholarship", ProgramCategory.SCHOLARSHIP,
                "Assistance for tuition and school fees of students enrolled in a college or university.",
                "Monthly household income at or below PHP 25,000 and current enrollment.",
                List.of("School ID", "Enrollment form"),
                "Submit this form in TAPAT and keep the reference number.",
                today.plusDays(60), "20000.00", "25000.00"));
        Program medical = programs.insert(program(admin, "Medical assistance", ProgramCategory.MEDICAL,
                "Help with hospital bills, medicines, and laboratory fees.",
                "Monthly household income at or below PHP 30,000 and a medical record for the expense.",
                List.of("Valid ID", "Medical abstract"),
                "Attach a valid ID and the medical abstract before the deadline.",
                today.plusDays(45), "15000.00", "30000.00"));
        programs.insert(program(admin, "Emergency relief", ProgramCategory.EMERGENCY,
                "Short-term aid for families affected by a sudden loss of income, fire, or disaster.",
                "Monthly household income at or below PHP 20,000 and residence in the community.",
                List.of("Valid ID", "Barangay certificate"),
                "Apply as soon as the need is known. Review is done by an administrator.",
                today.plusDays(5), "10000.00", "20000.00"));
        programs.insert(program(admin, "Livelihood starter", ProgramCategory.LIVELIHOOD,
                "Support for tools, materials, or a small stock that starts or restores a livelihood.",
                "Monthly household income at or below PHP 35,000 and a short description of the livelihood.",
                List.of("Valid ID", "Business proposal"),
                "Describe the livelihood and the items the amount will buy.",
                today.plusDays(90), "25000.00", "35000.00"));

        LocalDateTime submitted = LocalDateTime.now().minusDays(3).withNano(0);
        EligibilityReport scholarshipCheck = EligibilityChecker.standard().check(new EligibilityContext(
                scholarship, applicant.getId(), applicant.getFullName(), Money.parse("15000"),
                List.of("School ID", "Enrollment form"), List.of(), 0, today));
        Application approved = applications.insert(draft(applicant, scholarship, ApplicationStatus.APPROVED,
                Money.parse("15000"), "Tuition for the second semester.", Money.parse("12000"), Money.parse("10000"),
                scholarshipCheck, admin, "Approved for tuition.", submitted));
        storeSample(documentStore, documents, approved.getId(), "School ID");
        storeSample(documentStore, documents, approved.getId(), "Enrollment form");

        EligibilityReport medicalCheck = EligibilityChecker.standard().check(new EligibilityContext(
                medical, applicant.getId(), applicant.getFullName(), Money.parse("42000"),
                List.of("Valid ID"), List.of(), 0, today));
        Application pending = applications.insert(draft(applicant, medical, ApplicationStatus.PENDING,
                Money.parse("42000"), "Payment for a hospital bill.", Money.parse("8000"), null,
                medicalCheck, null, null, LocalDateTime.now().minusDays(1).withNano(0)));
        storeSample(documentStore, documents, pending.getId(), "Valid ID");

        funds.insert(scholarship.getId(), FundEntryType.ALLOCATION, Money.parse("500000"), null, "Semester fund", finance.getId());
        funds.insert(medical.getId(), FundEntryType.ALLOCATION, Money.parse("300000"), null, "Medical fund", finance.getId());
        Program emergencyFund = programNamed(programs, "Emergency relief");
        Program livelihoodFund = programNamed(programs, "Livelihood starter");
        if (emergencyFund != null) {
            funds.insert(emergencyFund.getId(), FundEntryType.ALLOCATION, Money.parse("150000"), null, "Emergency fund", finance.getId());
        }
        if (livelihoodFund != null) {
            funds.insert(livelihoodFund.getId(), FundEntryType.ALLOCATION, Money.parse("200000"), null, "Livelihood fund", finance.getId());
        }
        funds.insert(scholarship.getId(), FundEntryType.DISBURSEMENT, Money.parse("10000"), approved.getId(),
                "Tuition release", finance.getId());

        notifications.insert(applicant.getId(), "Application approved",
                approved.getReferenceNo() + " — Approved amount: PHP 10,000.00.");
        notifications.insert(applicant.getId(), "Check your application",
                pending.getReferenceNo() + "\n" + medicalCheck.notes());
        audit.insert(admin.getId(), "SEED", "system", null, "Demo records created");
    }

    public static void ensureClaimDemo(UserRepository users, ProgramRepository programs, ApplicationRepository applications,
                                       FundRepository funds, AuditRepository audit, PasswordHasher hasher,
                                       DocumentRepository documents, DocumentStore documentStore) {
        if (users.count() == 0) {
            return;
        }
        User finance = firstOfRole(users, Role.FINANCE_OFFICER);
        User admin = firstOfRole(users, Role.ADMINISTRATOR);
        if (finance == null || admin == null) {
            return;
        }
        for (User person : users.findAll()) {
            if ("applicant".equalsIgnoreCase(person.getUsername())) {
                users.saveProfile(person.getId(), LocalDate.of(1999, 3, 15), "Rizal");
            }
        }
        for (Application application : applications.findAll()) {
            if (application.getClaimReference() != null || application.getApprovedAmount() == null) {
                continue;
            }
            if (application.getStatus() != ApplicationStatus.APPROVED && application.getStatus() != ApplicationStatus.FOR_RELEASE) {
                continue;
            }
            if (funds.sumDisbursedForApplication(application.getId()).compareTo(application.getApprovedAmount()) < 0) {
                continue;
            }
            String claim = "TPT-R-" + String.format("%05d", application.getId());
            applications.issueClaim(application.getId(), claim);
            applications.markVerified(application.getId(), finance.getId());
            audit.insert(finance.getId(), "CLAIM_VERIFIED", "application", application.getId(), claim);
        }
        if (users.usernameExists("juan")) {
            return;
        }
        Program emergency = programNamed(programs, "Emergency relief");
        Program scholarship = programNamed(programs, "Tertiary scholarship");
        if (emergency == null || scholarship == null) {
            return;
        }
        User juan = user(users, hasher, "juan", "juan1234", "Juan Dela Cruz",
                "juan.delacruz@example.com", "09170001111", "14 San Roque, Quezon City", Role.APPLICANT);
        users.saveProfile(juan.getId(), LocalDate.of(1994, 6, 2), "San Roque");
        User lookalike = user(users, hasher, "juanm", "juan1234", "Juan M. Dela Cruz",
                "juanm.delacruz@example.com", "09170001111", "14 San Roque, Quezon City", Role.APPLICANT);
        users.saveProfile(lookalike.getId(), LocalDate.of(1994, 6, 2), "San Roque");
        EligibilityReport clear = new EligibilityReport(com.tapat.model.EligibilityStatus.PASS, List.of(), false);
        Application previous = applications.insert(draft(juan, emergency, ApplicationStatus.APPROVED,
                Money.parse("12000"), "Aid after a house fire.", Money.parse("5000"), Money.parse("5000"),
                clear, admin, "Released in the previous year.", LocalDateTime.now().minusDays(400).withNano(0)));
        funds.insert(emergency.getId(), FundEntryType.DISBURSEMENT, Money.parse("5000"), previous.getId(),
                "Previous emergency release", finance.getId());
        String previousClaim = "TPT-R-" + String.format("%05d", previous.getId());
        applications.issueClaim(previous.getId(), previousClaim);
        applications.markVerified(previous.getId(), finance.getId());
        audit.insert(finance.getId(), "CLAIM_VERIFIED", "application", previous.getId(), previousClaim);
        Application pending = applications.insert(draft(lookalike, scholarship, ApplicationStatus.PENDING,
                Money.parse("18000"), "Tuition for the first semester.", Money.parse("5000"), null,
                clear, null, null, LocalDateTime.now().minusHours(2).withNano(0)));
        storeSample(documentStore, documents, pending.getId(), "School ID");
        storeSample(documentStore, documents, pending.getId(), "Enrollment form");
    }

    private static Program programNamed(ProgramRepository programs, String name) {
        for (Program program : programs.findAll()) {
            if (name.equals(program.getName())) {
                return program;
            }
        }
        return null;
    }

    private static User firstOfRole(UserRepository users, Role role) {
        List<User> rows = users.findByRole(role);
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    private static User user(UserRepository users, PasswordHasher hasher, String username, String password,
                             String fullName, String email, String phone, String address, Role role) {
        StoredPassword stored = hasher.hash(password);
        return users.insert(username, stored.getHash(), stored.getSalt(), fullName, email, phone, address, role);
    }

    private static Program program(User admin, String name, ProgramCategory category, String description,
                                   String eligibility, List<String> documents, String instructions,
                                   LocalDate deadline, String maximum, String incomeLimit) {
        return new Program(0, name, category, description, eligibility, documents, instructions, deadline,
                Money.parse(maximum), Money.parse(incomeLimit), ProgramStatus.OPEN, admin.getId(), LocalDateTime.now().withNano(0));
    }

    private static Application draft(User applicant, Program program, ApplicationStatus status, BigDecimal income,
                                     String purpose, BigDecimal requested, BigDecimal approved, EligibilityReport report,
                                     User reviewer, String reviewNotes, LocalDateTime submitted) {
        return new Application(0, "", applicant.getId(), applicant.getFullName(), program.getId(), program.getName(),
                status, income, purpose, requested, approved, report.getStatus(), report.notes(), report.isDuplicate(),
                reviewer == null ? null : reviewer.getId(), reviewNotes, submitted, submitted, null, null, null);
    }

    private static void storeSample(DocumentStore store, DocumentRepository documents, int applicationId, String name) {
        try {
            Path file = Files.createTempFile("tapat-", ".txt");
            Files.writeString(file, name + " sample for application " + applicationId);
            String stored = store.save(applicationId, name, file);
            documents.insert(applicationId, name, stored);
            Files.deleteIfExists(file);
        } catch (Exception ex) {
            throw new com.tapat.exception.TapatException("Could not seed a sample document.", ex);
        }
    }
}
