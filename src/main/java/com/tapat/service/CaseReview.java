package com.tapat.service;

import com.tapat.model.Application;
import com.tapat.model.ApplicationDocument;
import com.tapat.model.ApplicationStatus;
import com.tapat.model.AuditLog;
import com.tapat.model.PersonRecord;
import com.tapat.model.Program;
import com.tapat.model.User;
import com.tapat.repository.ApplicationRepository;
import com.tapat.repository.AuditRepository;
import com.tapat.repository.DocumentRepository;
import com.tapat.repository.UserRepository;
import com.tapat.service.eligibility.EligibilityChecker;
import com.tapat.service.eligibility.EligibilityContext;
import com.tapat.service.eligibility.EligibilityReport;
import com.tapat.service.eligibility.RuleResult;
import com.tapat.util.Money;
import com.tapat.util.NameSimilarity;
import com.tapat.util.Text;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CaseReview {
    private final ApplicationRepository applications;
    private final DocumentRepository documents;
    private final UserRepository users;
    private final ProgramService programs;
    private final AuditRepository audit;
    private final EligibilityChecker checker;

    public CaseReview(ApplicationRepository applications, DocumentRepository documents, UserRepository users,
                      ProgramService programs, AuditRepository audit, EligibilityChecker checker) {
        this.applications = applications;
        this.documents = documents;
        this.users = users;
        this.programs = programs;
        this.audit = audit;
        this.checker = checker;
    }

    public Brief inspect(Application application) {
        Program program = programs.require(application.getProgramId());
        List<String> attached = new ArrayList<>();
        for (ApplicationDocument document : documents.findByApplication(application.getId())) {
            attached.add(document.getDocumentName());
        }
        EligibilityReport report = checker.check(new EligibilityContext(
                program,
                application.getApplicantId(),
                application.getApplicantName(),
                application.getHouseholdIncome(),
                attached,
                applications.findByProgram(application.getProgramId()),
                application.getId(),
                LocalDate.now()));
        List<PersonRecord> people = users.people();
        PersonRecord subject = null;
        for (PersonRecord person : people) {
            if (person.getId() == application.getApplicantId()) {
                subject = person;
                break;
            }
        }
        List<IdentityHit> hits = new ArrayList<>();
        if (subject != null) {
            for (PersonRecord other : people) {
                if (other.getId() == subject.getId()) {
                    continue;
                }
                IdentityHit hit = compare(subject, other);
                if (hit != null) {
                    hits.add(hit);
                }
            }
        }
        List<HistoryLine> history = new ArrayList<>();
        addHistory(history, application.getApplicantName(), applications.findByApplicant(application.getApplicantId()), application.getId());
        for (IdentityHit hit : hits) {
            addHistory(history, hit.name(), applications.findByApplicant(hit.personId()), application.getId());
        }
        int flags = 0;
        for (RuleResult check : report.getChecks()) {
            if (!check.isPassed()) {
                flags++;
            }
        }
        String recommendation;
        if (!hits.isEmpty()) {
            recommendation = "Possible duplicate. Read the integrity check, then decide. Do not reject this automatically.";
        } else if (flags > 0) {
            recommendation = flags + (flags == 1 ? " scorecard flag. " : " scorecard flags. ")
                    + "A person still has to approve or reject.";
        } else {
            recommendation = "No duplicate. Scorecard is clear. A person still has to decide.";
        }
        return new Brief(report.getChecks(), history, hits, recommendation);
    }

    public List<TraceLine> timeline(Application application) {
        List<TraceLine> lines = new ArrayList<>();
        lines.add(new TraceLine(Text.dateTime(application.getSubmittedAt()), "Application submitted", application.getApplicantName()));
        for (AuditLog log : audit.findFor("application", application.getId())) {
            if ("SUBMIT_APPLICATION".equals(log.getAction())) {
                continue;
            }
            lines.add(new TraceLine(Text.dateTime(log.getCreatedAt()), title(log.getAction()), detail(log, application)));
        }
        if (application.getClaimReference() != null && !hasTitle(lines, "Claim reference")) {
            lines.add(new TraceLine(Text.dateTime(application.getUpdatedAt()), "Claim reference generated", application.getClaimReference()));
        }
        if (application.getConfirmedAt() != null && !hasTitle(lines, "Assistance received")) {
            lines.add(new TraceLine(
                    Text.dateTime(application.getConfirmedAt()),
                    "Assistance received",
                    "Claim verified" + (application.getConfirmedByName() == null ? "" : " by " + application.getConfirmedByName())));
        }
        return lines;
    }

    private void addHistory(List<HistoryLine> history, String person, List<Application> rows, int skipId) {
        for (Application row : rows) {
            if (row.getId() == skipId || !row.getStatus().commitsFunds()) {
                continue;
            }
            BigDecimal amount = row.getApprovedAmount() == null ? row.getRequestedAmount() : row.getApprovedAmount();
            history.add(new HistoryLine(
                    person,
                    row.getProgramName(),
                    row.getSubmittedAt().getYear(),
                    Money.format(amount),
                    row.getStatus().getLabel()));
        }
    }

    private IdentityHit compare(PersonRecord subject, PersonRecord other) {
        int similarity = NameSimilarity.percent(subject.getFullName(), other.getFullName());
        String subjectPhone = NameSimilarity.digits(subject.getPhone());
        String otherPhone = NameSimilarity.digits(other.getPhone());
        boolean phone = subjectPhone.length() >= 7 && subjectPhone.equals(otherPhone);
        boolean birth = subject.getDateOfBirth() != null && subject.getDateOfBirth().equals(other.getDateOfBirth());
        boolean barangay = samePlace(subject, other);
        if (similarity < 88 && !phone && !(birth && barangay)) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Application row : applications.findByApplicant(other.getId())) {
            if (row.getStatus().commitsFunds() && row.getApprovedAmount() != null) {
                total = total.add(row.getApprovedAmount());
            }
        }
        return new IdentityHit(other.getId(), other.getFullName(), similarity, birth, phone, barangay, Money.format(total));
    }

    private boolean samePlace(PersonRecord subject, PersonRecord other) {
        String left = place(subject);
        String right = place(other);
        return !left.isBlank() && left.equals(right);
    }

    private String place(PersonRecord person) {
        if (person.getBarangay() != null && !person.getBarangay().isBlank()) {
            return Text.normalizeName(person.getBarangay());
        }
        String address = person.getAddress() == null ? "" : person.getAddress();
        int comma = address.indexOf(',');
        String head = comma > 0 ? address.substring(0, comma) : address;
        return Text.normalizeName(head);
    }

    private boolean hasTitle(List<TraceLine> lines, String text) {
        for (TraceLine line : lines) {
            if (line.title().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private String title(String action) {
        if ("SUBMIT_APPLICATION".equals(action)) {
            return "Application submitted";
        }
        if ("REVIEW_APPLICATION".equals(action)) {
            return "Application reviewed";
        }
        if ("DISBURSE".equals(action)) {
            return "Release authorized";
        }
        if ("CLAIM_ISSUED".equals(action)) {
            return "Claim reference generated";
        }
        if ("CLAIM_VERIFIED".equals(action)) {
            return "Assistance received";
        }
        return action.replace('_', ' ').toLowerCase();
    }

    private String detail(AuditLog log, Application application) {
        if ("CLAIM_VERIFIED".equals(log.getAction()) && application.getConfirmedByName() != null) {
            return Text.emptyIfNull(log.getDetails()) + " · " + application.getConfirmedByName();
        }
        User actor = log.getActorId() == null ? null : users.findById(log.getActorId()).orElse(null);
        String who = actor == null ? "" : actor.getFullName();
        String details = pretty(Text.emptyIfNull(log.getDetails()));
        if (who.isBlank()) {
            return details;
        }
        return details.isBlank() ? who : details + " · " + who;
    }

    private String pretty(String details) {
        if (details == null || details.isBlank()) {
            return "";
        }
        try {
            return ApplicationStatus.valueOf(details).getLabel();
        } catch (IllegalArgumentException ignored) {
            return details;
        }
    }

    public record Brief(List<RuleResult> checks, List<HistoryLine> history, List<IdentityHit> hits, String recommendation) {
    }

    public record HistoryLine(String person, String program, int year, String amount, String status) {
    }

    public record IdentityHit(int personId, String name, int similarity, boolean birthMatches, boolean phoneMatches,
                              boolean barangayMatches, String previousTotal) {
    }

    public record TraceLine(String when, String title, String detail) {
    }
}
