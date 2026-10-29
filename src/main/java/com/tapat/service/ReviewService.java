package com.tapat.service;

import com.tapat.exception.AuthorizationException;
import com.tapat.exception.ValidationException;
import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;
import com.tapat.model.FundEntryType;
import com.tapat.model.Program;
import com.tapat.model.User;
import com.tapat.repository.ApplicationRepository;
import com.tapat.repository.AuditRepository;
import com.tapat.repository.FundRepository;
import com.tapat.util.Money;
import com.tapat.util.Text;

import java.math.BigDecimal;

public class ReviewService {
    private final ApplicationRepository applications;
    private final ApplicationService applicationService;
    private final ProgramService programs;
    private final NotificationService notifications;
    private final AuditRepository audit;
    private final FundRepository funds;

    public ReviewService(ApplicationRepository applications, ApplicationService applicationService,
                         ProgramService programs, NotificationService notifications, AuditRepository audit,
                         FundRepository funds) {
        this.applications = applications;
        this.applicationService = applicationService;
        this.programs = programs;
        this.notifications = notifications;
        this.audit = audit;
        this.funds = funds;
    }

    public void markUnderReview(User actor, int applicationId, String notes) {
        Application application = requireReview(actor, applicationId);
        if (application.getStatus().isDecided()) {
            throw new ValidationException("This application is already decided.");
        }
        save(actor, application, ApplicationStatus.UNDER_REVIEW, application.getApprovedAmount(), notes,
                "Under review", "Your application is under review.");
    }

    public void approve(User actor, int applicationId, BigDecimal amount, String notes) {
        Application application = requireReview(actor, applicationId);
        if (application.getStatus() == ApplicationStatus.REJECTED) {
            throw new ValidationException("Rejected applications cannot be approved.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Enter an approved amount greater than zero.");
        }
        Program program = programs.require(application.getProgramId());
        if (program.getMaxAssistance() != null && amount.compareTo(program.getMaxAssistance()) > 0) {
            throw new ValidationException("Amount exceeds the program maximum of " + Money.format(program.getMaxAssistance()) + ".");
        }
        BigDecimal reserved = BigDecimal.ZERO;
        for (Application other : applications.findByProgram(application.getProgramId())) {
            if (other.getId() == application.getId() || !other.getStatus().commitsFunds() || other.getApprovedAmount() == null) {
                continue;
            }
            reserved = reserved.add(other.getApprovedAmount());
        }
        BigDecimal allocated = funds.sum(program.getId(), FundEntryType.ALLOCATION);
        BigDecimal available = allocated.subtract(reserved);
        if (amount.compareTo(available) > 0) {
            throw new ValidationException("Only " + Money.format(available)
                    + " is still unreserved. Approving this amount would commit more than the program holds.");
        }
        save(actor, application, ApplicationStatus.FOR_RELEASE, amount, notes,
                "Approved for release", "Approved amount: " + Money.format(amount)
                        + ". The amount is reserved until finance releases it.");
    }

    public void reject(User actor, int applicationId, String notes) {
        Application application = requireReview(actor, applicationId);
        if (application.getStatus().commitsFunds()) {
            throw new ValidationException("This application already reserves funds and cannot be rejected here.");
        }
        if (Text.trimToNull(notes) == null) {
            throw new ValidationException("Enter a reason for the rejection.");
        }
        save(actor, application, ApplicationStatus.REJECTED, null, notes,
                "Application rejected", notes.trim());
    }

    private Application requireReview(User actor, int applicationId) {
        if (actor == null || !actor.canReviewApplications()) {
            throw new AuthorizationException("You cannot review applications.");
        }
        return applicationService.require(applicationId);
    }

    private void save(User actor, Application application, ApplicationStatus status, BigDecimal approvedAmount,
                      String notes, String title, String message) {
        applications.updateReview(application.getId(), status, approvedAmount, actor.getId(), Text.trimToNull(notes));
        notifications.notify(application.getApplicantId(), title,
                application.getReferenceNo() + " — " + message);
        audit.insert(actor.getId(), "REVIEW_APPLICATION", "application", application.getId(), status.name());
    }
}
