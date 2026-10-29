package com.tapat.service;

import com.tapat.exception.AuthorizationException;
import com.tapat.exception.ValidationException;
import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;
import com.tapat.model.FundEntry;
import com.tapat.model.FundEntryType;
import com.tapat.model.Program;
import com.tapat.model.ProgramBalance;
import com.tapat.model.User;
import com.tapat.repository.ApplicationRepository;
import com.tapat.repository.AuditRepository;
import com.tapat.repository.FundRepository;
import com.tapat.repository.ProgramRepository;
import com.tapat.repository.sqlite.Database;
import com.tapat.util.Money;
import com.tapat.util.Text;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FundService {
    private final Database database;
    private final FundRepository funds;
    private final ProgramRepository programs;
    private final ApplicationRepository applications;
    private final NotificationService notifications;
    private final AuditRepository audit;

    public FundService(Database database, FundRepository funds, ProgramRepository programs,
                       ApplicationRepository applications, NotificationService notifications, AuditRepository audit) {
        this.database = database;
        this.funds = funds;
        this.programs = programs;
        this.applications = applications;
        this.notifications = notifications;
        this.audit = audit;
    }

    public List<ProgramBalance> summaries() {
        List<ProgramBalance> rows = new ArrayList<>();
        for (Program program : programs.findAll()) {
            ProgramBalance balance = balance(program);
            if (program.getStatus() == com.tapat.model.ProgramStatus.OPEN
                    || balance.getAllocated().signum() > 0
                    || balance.getDisbursed().signum() > 0) {
                rows.add(balance);
            }
        }
        return rows;
    }

    public List<FundEntry> ledger(User actor) {
        if (actor == null || (!actor.canRecordFunds() && !actor.canExportFullReports())) {
            throw new AuthorizationException("You cannot view the fund ledger.");
        }
        return funds.findAll();
    }

    public List<Application> payable(User actor) {
        if (actor == null || !actor.canRecordFunds()) {
            throw new AuthorizationException("You cannot record disbursements.");
        }
        List<Application> approved = new ArrayList<>();
        for (Application application : applications.findAll()) {
            if ((application.getStatus() == ApplicationStatus.APPROVED || application.getStatus() == ApplicationStatus.FOR_RELEASE)
                    && unpaid(application).signum() > 0) {
                approved.add(application);
            }
        }
        return approved;
    }

    public void allocate(User actor, int programId, BigDecimal amount, String remarks) {
        if (actor == null || !actor.canRecordFunds()) {
            throw new AuthorizationException("You cannot record fund allocations.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Enter an amount greater than zero.");
        }
        Optional<Program> found = programs.findById(programId);
        if (found.isEmpty()) {
            throw new ValidationException("That program was not found.");
        }
        Program program = found.get();
        database.transaction(() -> {
            funds.insert(program.getId(), FundEntryType.ALLOCATION, amount, null, Text.trimToNull(remarks), actor.getId());
            audit.insert(actor.getId(), "ALLOCATE_FUNDS", "program", program.getId(), Money.format(amount));
        });
    }

    public void disburse(User actor, int applicationId, BigDecimal amount, String remarks) {
        if (actor == null || !actor.canRecordFunds()) {
            throw new AuthorizationException("You cannot record disbursements.");
        }
        database.transaction(() -> {
            Optional<Application> found = applications.findById(applicationId);
            if (found.isEmpty()) {
                throw new ValidationException("That application was not found.");
            }
            Application application = found.get();
            if ((application.getStatus() != ApplicationStatus.APPROVED && application.getStatus() != ApplicationStatus.FOR_RELEASE)
                    || application.getApprovedAmount() == null) {
                throw new ValidationException("A release can be recorded only after a person has approved the application.");
            }
            BigDecimal unpaid = application.getApprovedAmount().subtract(funds.sumDisbursedForApplication(application.getId()));
            BigDecimal remaining = BalanceCalculator.remaining(
                    funds.sum(application.getProgramId(), FundEntryType.ALLOCATION),
                    funds.sum(application.getProgramId(), FundEntryType.DISBURSEMENT));
            BalanceCalculator.checkDisbursement(amount, unpaid, remaining);
            funds.insert(application.getProgramId(), FundEntryType.DISBURSEMENT, amount, application.getId(),
                    Text.trimToNull(remarks), actor.getId());
            BigDecimal stillUnpaid = application.getApprovedAmount().subtract(
                    funds.sumDisbursedForApplication(application.getId()));
            String claim = application.getClaimReference();
            if (stillUnpaid.signum() <= 0) {
                claim = "TPT-R-" + String.format("%05d", application.getId());
                applications.issueClaim(application.getId(), claim);
                audit.insert(actor.getId(), "CLAIM_ISSUED", "application", application.getId(), claim);
            }
            notifications.notify(application.getApplicantId(), "Assistance released",
                    application.getReferenceNo() + " — " + Money.format(amount) + " was released."
                            + (claim == null ? "" : " Claim reference: " + claim + "."));
            audit.insert(actor.getId(), "DISBURSE", "application", application.getId(), Money.format(amount));
        });
    }

    public Application verifyClaim(User actor, String claimReference) {
        if (actor == null || !actor.canRecordFunds()) {
            throw new AuthorizationException("You cannot verify a claim.");
        }
        String reference = Text.trimToNull(claimReference);
        if (reference == null) {
            throw new ValidationException("Enter the claim reference.");
        }
        Optional<Application> found = applications.findByClaim(reference);
        if (found.isEmpty()) {
            throw new ValidationException("No release uses that claim reference.");
        }
        Application application = found.get();
        if (application.getStatus() == ApplicationStatus.VERIFIED) {
            throw new ValidationException("This claim was already verified.");
        }
        if (application.getStatus() != ApplicationStatus.RELEASED) {
            throw new ValidationException("Verify a claim only after the assistance has been released.");
        }
        database.transaction(() -> {
            applications.markVerified(application.getId(), actor.getId());
            notifications.notify(application.getApplicantId(), "Assistance verified",
                    application.getReferenceNo() + " — receipt confirmed for " + application.getClaimReference() + ".");
            audit.insert(actor.getId(), "CLAIM_VERIFIED", "application", application.getId(), application.getClaimReference());
        });
        Optional<Application> saved = applications.findById(application.getId());
        if (saved.isEmpty()) {
            throw new ValidationException("That application was not found.");
        }
        return saved.get();
    }

    public BigDecimal unpaid(Application application) {
        if (application.getApprovedAmount() == null) {
            return Money.ZERO;
        }
        return application.getApprovedAmount().subtract(funds.sumDisbursedForApplication(application.getId()));
    }

    private ProgramBalance balance(Program program) {
        BigDecimal approvedTotal = Money.ZERO;
        BigDecimal confirmedTotal = Money.ZERO;
        BigDecimal awaitingTotal = Money.ZERO;
        int count = 0;
        for (Application application : applications.findByProgram(program.getId())) {
            if (!application.getStatus().commitsFunds() || application.getApprovedAmount() == null) {
                continue;
            }
            approvedTotal = approvedTotal.add(application.getApprovedAmount());
            count++;
            if (application.getStatus() == ApplicationStatus.VERIFIED) {
                confirmedTotal = confirmedTotal.add(application.getApprovedAmount());
            } else if (application.getStatus() == ApplicationStatus.RELEASED) {
                awaitingTotal = awaitingTotal.add(application.getApprovedAmount());
            }
        }
        return new ProgramBalance(
                program.getId(),
                program.getName(),
                funds.sum(program.getId(), FundEntryType.ALLOCATION),
                funds.sum(program.getId(), FundEntryType.DISBURSEMENT),
                count,
                approvedTotal,
                confirmedTotal,
                awaitingTotal);
    }
}
