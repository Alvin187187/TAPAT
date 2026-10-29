package com.tapat.service;

import com.tapat.exception.AuthorizationException;
import com.tapat.exception.ValidationException;
import com.tapat.model.Application;
import com.tapat.model.ApplicationDocument;
import com.tapat.model.ApplicationStatus;
import com.tapat.model.Program;
import com.tapat.model.Role;
import com.tapat.model.User;
import com.tapat.repository.ApplicationRepository;
import com.tapat.repository.AuditRepository;
import com.tapat.repository.DocumentRepository;
import com.tapat.service.eligibility.EligibilityChecker;
import com.tapat.service.eligibility.EligibilityContext;
import com.tapat.service.eligibility.EligibilityReport;
import com.tapat.util.Money;
import com.tapat.util.Text;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;

public class ApplicationService {
    private final ApplicationRepository applications;
    private final DocumentRepository documents;
    private final ProgramService programs;
    private final AuditRepository audit;
    private final NotificationService notifications;
    private final EligibilityChecker checker;
    private final DocumentStore documentStore;

    public ApplicationService(ApplicationRepository applications, DocumentRepository documents, ProgramService programs,
                              AuditRepository audit, NotificationService notifications, EligibilityChecker checker,
                              DocumentStore documentStore) {
        this.applications = applications;
        this.documents = documents;
        this.programs = programs;
        this.audit = audit;
        this.notifications = notifications;
        this.checker = checker;
        this.documentStore = documentStore;
    }

    public Application submit(User actor, int programId, BigDecimal householdIncome, BigDecimal requestedAmount,
                              String purpose, Map<String, Path> attachments) {
        if (actor == null || !actor.canSubmitApplications()) {
            throw new AuthorizationException("Sign in as an applicant to submit an application.");
        }
        Program program = programs.require(programId);
        programs.requireOpen(program);
        String reason = Text.trimToNull(purpose);
        if (reason == null) {
            throw new ValidationException("Describe why you are applying.");
        }
        if (requestedAmount == null || requestedAmount.signum() <= 0) {
            throw new ValidationException("Enter a requested amount greater than zero.");
        }
        if (program.getMaxAssistance() != null && requestedAmount.compareTo(program.getMaxAssistance()) > 0) {
            throw new ValidationException("Requested amount exceeds the program maximum of "
                    + Money.format(program.getMaxAssistance()) + ".");
        }
        List<String> attachedNames = new ArrayList<>();
        if (attachments != null) {
            attachedNames.addAll(attachments.keySet());
        }
        EligibilityReport report = checker.check(new EligibilityContext(
                program,
                actor.getId(),
                actor.getFullName(),
                householdIncome,
                attachedNames,
                applications.findByProgram(programId),
                0,
                LocalDate.now()));
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Application saved = applications.insert(new Application(
                0,
                "",
                actor.getId(),
                actor.getFullName(),
                program.getId(),
                program.getName(),
                ApplicationStatus.PENDING,
                householdIncome,
                reason,
                requestedAmount,
                null,
                report.getStatus(),
                report.notes(),
                report.isDuplicate(),
                null,
                null,
                now,
                now,
                null,
                null,
                null));
        if (attachments != null) {
            for (Map.Entry<String, Path> entry : attachments.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                String stored = documentStore.save(saved.getId(), entry.getKey(), entry.getValue());
                documents.insert(saved.getId(), entry.getKey(), stored);
            }
        }
        notifications.notify(actor.getId(), "Application submitted",
                saved.getReferenceNo() + " for " + program.getName() + " is pending review.");
        if (!report.getMessages().isEmpty()) {
            notifications.notify(actor.getId(), "Check your application",
                    saved.getReferenceNo() + "\n" + report.notes());
        }
        notifications.notifyRole(Role.ADMINISTRATOR, "New application",
                saved.getReferenceNo() + " was submitted for " + program.getName() + ".");
        audit.insert(actor.getId(), "SUBMIT_APPLICATION", "application", saved.getId(), saved.getReferenceNo());
        return applications.findById(saved.getId()).orElse(saved);
    }

    public EligibilityReport preview(User actor, int programId, BigDecimal householdIncome, List<String> documentNames) {
        if (actor == null || !actor.canSubmitApplications()) {
            throw new AuthorizationException("Sign in as an applicant to check an application.");
        }
        Program program = programs.require(programId);
        return checker.check(new EligibilityContext(
                program,
                actor.getId(),
                actor.getFullName(),
                householdIncome,
                documentNames == null ? List.of() : documentNames,
                applications.findByProgram(programId),
                0,
                LocalDate.now()));
    }

    public List<Application> mine(User actor) {
        if (actor == null || !actor.canSubmitApplications()) {
            throw new AuthorizationException("Sign in as an applicant to view applications.");
        }
        return applications.findByApplicant(actor.getId());
    }

    public List<Application> all(User actor) {
        if (actor == null || !actor.canReviewApplications()) {
            throw new AuthorizationException("You cannot review applications.");
        }
        return applications.findAll();
    }

    public List<Application> traceable(User actor) {
        if (actor == null || !(actor.canReviewApplications() || actor.canRecordFunds() || actor.canExportFullReports())) {
            throw new AuthorizationException("You cannot open an assistance trace.");
        }
        return applications.findAll();
    }

    public Application require(int id) {
        Optional<Application> found = applications.findById(id);
        if (found.isEmpty()) {
            throw new ValidationException("That application was not found.");
        }
        return found.get();
    }

    public java.util.Optional<Application> findByNotice(User actor, String token) {
        if (actor == null || token == null || token.isBlank()) {
            return java.util.Optional.empty();
        }
        String needle = token.trim();
        java.util.List<Application> rows;
        if (actor.canReviewApplications() || actor.canRecordFunds() || actor.canExportFullReports()) {
            rows = applications.findAll();
        } else if (actor.canSubmitApplications()) {
            rows = applications.findByApplicant(actor.getId());
        } else {
            return java.util.Optional.empty();
        }
        for (Application row : rows) {
            if (needle.equalsIgnoreCase(row.getReferenceNo())
                    || (row.getClaimReference() != null && needle.equalsIgnoreCase(row.getClaimReference()))) {
                return java.util.Optional.of(row);
            }
        }
        return java.util.Optional.empty();
    }

    public List<ApplicationDocument> documentsFor(int applicationId) {
        return documents.findByApplication(applicationId);
    }

    public boolean canView(User actor, Application application) {
        if (actor == null || application == null) {
            return false;
        }
        return actor.getId() == application.getApplicantId()
                || actor.canReviewApplications()
                || actor.canRecordFunds()
                || actor.canExportFullReports();
    }
}
