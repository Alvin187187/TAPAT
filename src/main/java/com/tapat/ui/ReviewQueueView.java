package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.Application;
import com.tapat.model.ApplicationDocument;
import com.tapat.model.ApplicationStatus;
import com.tapat.model.ProgramBalance;
import com.tapat.model.User;
import com.tapat.service.CaseReview;
import com.tapat.service.eligibility.RuleResult;
import com.tapat.util.Money;
import com.tapat.util.Text;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReviewQueueView {
    private final AppContext context;
    private final Session session;
    private final TextField search = UiKit.textField();
    private final ComboBox<String> filter = UiKit.combo(List.of(
            "All", "Pending", "Under review", "For release", "Released", "Verified", "Approved", "Rejected"));
    private final TableView<Application> table = UiKit.table("No applications in this view.");
    private final VBox detail = new VBox(12);
    private final TextArea notes = UiKit.textArea(3);
    private final TextField amount = UiKit.textField();
    private final Label error = UiKit.error();
    private final Label finding = UiKit.finding("");
    private final BorderPane root;
    private List<Application> all = List.of();
    private String pendingReference;

    public ReviewQueueView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        search.setPromptText("Search reference, name, or program");
        search.setPrefWidth(280);
        search.textProperty().addListener((obs, previous, current) -> applyFilter());
        filter.setValue("All");
        filter.setOnAction(event -> applyFilter());
        table.getColumns().add(UiKit.column("Reference", Application::getReferenceNo));
        table.getColumns().add(UiKit.column("Applicant", Application::getApplicantName));
        table.getColumns().add(UiKit.column("Program", Application::getProgramName));
        table.getColumns().add(UiKit.statusColumn());
        table.getColumns().add(UiKit.column("Check", this::flagText));
        table.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> show(selected));
        Button underReview = UiKit.secondary("Mark under review");
        underReview.setOnAction(event -> decide(ApplicationStatus.UNDER_REVIEW));
        Button approve = UiKit.primary("Approve");
        approve.setOnAction(event -> decide(ApplicationStatus.APPROVED));
        Button reject = UiKit.danger("Reject");
        reject.setOnAction(event -> decide(ApplicationStatus.REJECTED));
        detail.getStyleClass().add("panel");
        UiKit.watch(detail);
        VBox decision = new VBox(12,
                UiKit.section("Decision"),
                UiKit.body("Approve reserves the amount. It does not pay anyone. A duplicate flag never auto-rejects."),
                UiKit.field("Review notes", notes),
                UiKit.field("Approved amount", amount),
                error,
                UiKit.actions(underReview, approve, reject));
        decision.getStyleClass().add("panel");
        VBox side = new VBox(10, detail, decision);
        side.setMinWidth(0);
        VBox list = new VBox(8, UiKit.toolbar(search, filter), table);
        list.setMinWidth(0);
        table.setMinWidth(0);
        UiKit.grow(list);
        this.root = UiKit.workspace(
                UiKit.split(list, UiKit.sideScroll(side)),
                UiKit.title("Review cases"),
                UiKit.body("Your job: decide. Approve reserves money for finance. A duplicate flag means read it — it never auto-rejects."),
                finding);
    }

    public Parent getRoot() {
        return root;
    }

    public void selectReference(String reference) {
        this.pendingReference = reference;
    }

    public void refresh() {
        UiKit.clear(error);
        if (session.current().isEmpty()) {
            return;
        }
        try {
            all = context.applications().all(session.current().get());
            long waiting = all.stream().filter(item -> item.getStatus() == ApplicationStatus.PENDING
                    || item.getStatus() == ApplicationStatus.UNDER_REVIEW).count();
            finding.setText(waiting == 0
                    ? "No application is waiting for a decision."
                    : waiting + (waiting == 1 ? " application is" : " applications are") + " waiting for a decision.");
            applyFilter();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private void applyFilter() {
        String selected = filter.getValue();
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        List<Application> shown = new ArrayList<>();
        for (Application application : all) {
            boolean statusMatch = "All".equals(selected) || selected == null
                    || application.getStatus().getLabel().equals(selected);
            boolean textMatch = query.isEmpty()
                    || application.getReferenceNo().toLowerCase().contains(query)
                    || application.getApplicantName().toLowerCase().contains(query)
                    || application.getProgramName().toLowerCase().contains(query);
            if (statusMatch && textMatch) {
                shown.add(application);
            }
        }
        Application current = table.getSelectionModel().getSelectedItem();
        table.getItems().setAll(shown);
        UiKit.fit(table);
        if (pendingReference != null) {
            String wanted = pendingReference;
            pendingReference = null;
            shown.stream().filter(item -> wanted.equalsIgnoreCase(item.getReferenceNo())).findFirst()
                    .ifPresentOrElse(table.getSelectionModel()::select, () -> pick(shown, current));
        } else {
            pick(shown, current);
        }
    }

    private void pick(List<Application> shown, Application current) {
        if (current != null) {
            shown.stream().filter(item -> item.getId() == current.getId()).findFirst()
                    .ifPresentOrElse(table.getSelectionModel()::select, () -> {
                        if (!shown.isEmpty()) {
                            table.getSelectionModel().select(0);
                        } else {
                            show(null);
                        }
                    });
        } else if (!shown.isEmpty()) {
            table.getSelectionModel().select(0);
        } else {
            show(null);
        }
    }

    private void show(Application application) {
        detail.getChildren().clear();
        if (application == null) {
            detail.getChildren().add(UiKit.body("Select an application on the left. The applicant, the papers, and any flags appear here before you decide."));
            return;
        }
        User applicant = context.auth().requireUser(application.getApplicantId());
        detail.getChildren().add(UiKit.section(application.getReferenceNo()));
        detail.getChildren().add(UiKit.badge(application.getStatus()));
        detail.getChildren().add(UiKit.finding(flagText(application).isBlank()
                ? "No eligibility flag on this application."
                : flagText(application) + " — read the notes before deciding."));
        detail.getChildren().add(UiKit.fact("Applicant", application.getApplicantName()));
        detail.getChildren().add(UiKit.fact("Email", Text.emptyIfNull(applicant.getEmail())));
        detail.getChildren().add(UiKit.fact("Phone", Text.emptyIfNull(applicant.getPhone())));
        detail.getChildren().add(UiKit.fact("Address", Text.emptyIfNull(applicant.getAddress())));
        detail.getChildren().add(UiKit.fact("Program", application.getProgramName()));
        detail.getChildren().add(UiKit.fact("Income", application.getHouseholdIncome() == null
                ? "Not provided" : Money.format(application.getHouseholdIncome())));
        detail.getChildren().add(UiKit.fact("Requested", Money.format(application.getRequestedAmount())));
        for (ProgramBalance balance : context.funds().summaries()) {
            if (balance.getProgramId() == application.getProgramId()) {
                detail.getChildren().add(UiKit.fact("Still unreserved", Money.format(balance.getUnreserved())));
            }
        }
        CaseReview.Brief brief = context.cases().inspect(application);
        detail.getChildren().add(UiKit.section("Eligibility scorecard"));
        for (RuleResult check : brief.checks()) {
            detail.getChildren().add(UiKit.scoreLine(check.isPassed(), check.getName(), check.getDetail()));
        }
        detail.getChildren().add(UiKit.section("Assistance history"));
        if (brief.history().isEmpty()) {
            detail.getChildren().add(UiKit.meta("No earlier assistance is on record for this person."));
        } else {
            for (CaseReview.HistoryLine line : brief.history()) {
                detail.getChildren().add(UiKit.historyLine(
                        String.valueOf(line.year()), line.program(), line.amount(), line.status(), line.person()));
            }
        }
        detail.getChildren().add(UiKit.section("Integrity check"));
        if (brief.hits().isEmpty()) {
            detail.getChildren().add(UiKit.meta("No other person matches this name, birth date, phone, or barangay."));
        } else {
            for (CaseReview.IdentityHit hit : brief.hits()) {
                Label warn = UiKit.finding("Possible duplicate: " + hit.name()
                        + ". Name similarity " + hit.similarity() + "%."
                        + " Date of birth " + (hit.birthMatches() ? "matches" : "does not match") + "."
                        + " Phone " + (hit.phoneMatches() ? "matches" : "does not match") + "."
                        + " Barangay " + (hit.barangayMatches() ? "matches" : "does not match") + "."
                        + " Previous assistance " + hit.previousTotal() + ".");
                warn.getStyleClass().add("finding-warn");
                detail.getChildren().add(warn);
            }
        }
        Label call = UiKit.finding(brief.recommendation());
        if (!brief.hits().isEmpty()) {
            call.getStyleClass().add("finding-warn");
        }
        detail.getChildren().add(call);
        detail.getChildren().add(UiKit.section("Audit timeline"));
        for (CaseReview.TraceLine line : context.cases().timeline(application)) {
            detail.getChildren().add(UiKit.traceLine(line.when(), line.title(), line.detail()));
        }
        detail.getChildren().add(UiKit.section("Purpose"));
        detail.getChildren().add(UiKit.body(application.getPurpose()));
        if (application.getEligibilityNotes() != null && !application.getEligibilityNotes().isBlank()) {
            detail.getChildren().add(UiKit.section("Flags"));
            detail.getChildren().add(UiKit.body(application.getEligibilityNotes()));
        }
        for (ApplicationDocument document : context.applications().documentsFor(application.getId())) {
            Button open = UiKit.secondary("Open " + document.getDocumentName());
            open.setOnAction(event -> openDocument(document));
            detail.getChildren().add(open);
        }
        notes.setText(Text.emptyIfNull(application.getReviewNotes()));
        amount.setText(application.getApprovedAmount() == null ? "" : application.getApprovedAmount().toPlainString());
    }

    private void openDocument(ApplicationDocument document) {
        UiKit.clear(error);
        try {
            Path path = context.documents().resolve(document.getStoredPath());
            Desktop.getDesktop().open(path.toFile());
        } catch (Exception ex) {
            error.setText("Could not open " + document.getDocumentName() + ".");
            error.setVisible(true);
            error.setManaged(true);
        }
    }

    private void decide(ApplicationStatus status) {
        UiKit.clear(error);
        Application application = table.getSelectionModel().getSelectedItem();
        if (application == null || session.current().isEmpty()) {
            return;
        }
        String message;
        if (status == ApplicationStatus.APPROVED) {
            message = "Approve " + application.getReferenceNo() + " and reserve this amount for release?";
        } else if (status == ApplicationStatus.REJECTED) {
            message = "Reject " + application.getReferenceNo() + "?";
        } else {
            message = "Mark " + application.getReferenceNo() + " under review?";
        }
        if (!UiKit.confirm(table.getScene() == null ? null : table.getScene().getWindow(), message)) {
            return;
        }
        try {
            User actor = session.current().get();
            if (status == ApplicationStatus.APPROVED) {
                context.review().approve(actor, application.getId(), Money.requirePositive(amount.getText(), "approved amount"), notes.getText());
            } else if (status == ApplicationStatus.REJECTED) {
                context.review().reject(actor, application.getId(), notes.getText());
            } else {
                context.review().markUnderReview(actor, application.getId(), notes.getText());
            }
            context.changed();
            refresh();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private String flagText(Application application) {
        if (application.isDuplicateFlag()) {
            return "Duplicate";
        }
        if (application.getEligibilityStatus() == com.tapat.model.EligibilityStatus.FLAG) {
            return "Flagged";
        }
        return "";
    }
}
