package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.AppModule;
import com.tapat.model.Application;
import com.tapat.model.ApplicationDocument;
import com.tapat.model.ApplicationStatus;
import com.tapat.service.CaseReview;
import com.tapat.util.Money;
import com.tapat.util.Text;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

public class MyApplicationsView {
    private final AppContext context;
    private final Session session;
    private final TableView<Application> table = UiKit.table("You have not submitted an application.");
    private final VBox detail = new VBox(12);
    private final Label error = UiKit.error();
    private final Label finding = UiKit.finding("");
    private final BorderPane root;
    private String pendingReference;

    public MyApplicationsView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        table.getColumns().add(UiKit.column("Reference", Application::getReferenceNo));
        table.getColumns().add(UiKit.column("Program", Application::getProgramName));
        table.getColumns().add(UiKit.statusColumn());
        table.getColumns().add(UiKit.moneyColumn("Requested", application -> Money.compact(application.getRequestedAmount())));
        table.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> show(selected));
        Button apply = UiKit.primary("Apply for assistance");
        apply.setOnAction(event -> navigator.show(AppModule.DIRECTORY));
        detail.getStyleClass().add("panel");
        UiKit.watch(detail);
        VBox list = new VBox(8, table);
        list.setMinWidth(0);
        table.setMinWidth(0);
        UiKit.grow(list);
        this.root = UiKit.workspace(
                UiKit.split(list, UiKit.sideScroll(detail)),
                UiKit.pageHeader("My cases", apply),
                finding,
                error);
    }

    public Parent getRoot() {
        return root;
    }

    public void selectReference(String reference) {
        this.pendingReference = reference;
    }

    public void refresh() {
        UiKit.clear(error);
        detail.getChildren().clear();
        if (session.current().isEmpty()) {
            finding.setText("");
            return;
        }
        try {
            List<Application> mine = context.applications().mine(session.current().get());
            table.getItems().setAll(mine);
            long waiting = mine.stream().filter(item ->
                    item.getStatus() == ApplicationStatus.PENDING || item.getStatus() == ApplicationStatus.UNDER_REVIEW).count();
            if (mine.isEmpty()) {
                finding.setText("No case yet. Apply for assistance, then watch Submitted → For release → Released → Verified.");
            } else if (waiting == 0) {
                finding.setText("Select a case. Verified means the money was confirmed received.");
            } else {
                finding.setText(waiting + (waiting == 1 ? " case is" : " cases are")
                        + " with a reviewer. Open a row to see where it is.");
            }
            if (pendingReference != null) {
                String wanted = pendingReference;
                pendingReference = null;
                mine.stream().filter(item -> wanted.equalsIgnoreCase(item.getReferenceNo())).findFirst()
                        .ifPresentOrElse(table.getSelectionModel()::select, () -> {
                            if (!mine.isEmpty()) {
                                table.getSelectionModel().select(0);
                            }
                        });
            } else if (!mine.isEmpty()) {
                table.getSelectionModel().select(0);
            } else {
                show(null);
            }
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private void show(Application application) {
        detail.getChildren().clear();
        if (application == null || session.current().isEmpty()) {
            detail.getChildren().add(UiKit.body("No case yet. Click Apply for assistance, pick a program, submit."));
            return;
        }
        if (!context.applications().canView(session.current().get(), application)) {
            return;
        }
        BigDecimal disbursed = disbursed(application);
        detail.getChildren().add(UiKit.section(application.getReferenceNo()));
        detail.getChildren().add(UiKit.badge(application.getStatus()));
        detail.getChildren().add(UiKit.assistanceTrack(application.getStatus(), application.getApprovedAmount(), disbursed));
        detail.getChildren().add(UiKit.body(application.getProgramName()));
        detail.getChildren().add(UiKit.fact("Requested", Money.format(application.getRequestedAmount())));
        detail.getChildren().add(UiKit.fact("Approved", application.getApprovedAmount() == null
                ? "Not decided" : Money.format(application.getApprovedAmount())));
        detail.getChildren().add(UiKit.fact("Released", disbursed == null ? "Not released" : Money.format(disbursed)));
        if (application.getApprovedAmount() != null && disbursed != null) {
            detail.getChildren().add(UiKit.fact("Still to release", Money.format(application.getApprovedAmount().subtract(disbursed))));
        }
        if (application.getClaimReference() != null) {
            detail.getChildren().add(UiKit.section("TAPAT assistance release"));
            detail.getChildren().add(UiKit.fact("Reference", application.getClaimReference()));
            detail.getChildren().add(UiKit.fact("Approved amount", Money.format(application.getApprovedAmount())));
            String releaseStatus = application.getStatus() == ApplicationStatus.VERIFIED
                    ? "Verified received"
                    : "Awaiting beneficiary confirmation";
            detail.getChildren().add(UiKit.fact("Release status", releaseStatus));
            if (application.getConfirmedAt() != null) {
                detail.getChildren().add(UiKit.fact("Confirmed", Text.dateTime(application.getConfirmedAt())));
            }
            detail.getChildren().add(UiKit.body("Present this reference to the finance officer. The case stays released until that officer verifies it."));
        }
        detail.getChildren().add(UiKit.section("Audit timeline"));
        for (CaseReview.TraceLine line : context.cases().timeline(application)) {
            detail.getChildren().add(UiKit.traceLine(line.when(), line.title(), line.detail()));
        }
        detail.getChildren().add(UiKit.section("Purpose"));
        detail.getChildren().add(UiKit.body(application.getPurpose()));
        if (application.getEligibilityNotes() != null && !application.getEligibilityNotes().isBlank()) {
            detail.getChildren().add(UiKit.section("Sent to the reviewer"));
            detail.getChildren().add(UiKit.body(application.getEligibilityNotes()));
        }
        if (application.getReviewNotes() != null && !application.getReviewNotes().isBlank()) {
            detail.getChildren().add(UiKit.section("Reviewer note"));
            detail.getChildren().add(UiKit.body(application.getReviewNotes()));
        }
        List<ApplicationDocument> files = context.applications().documentsFor(application.getId());
        if (!files.isEmpty()) {
            detail.getChildren().add(UiKit.section("Documents"));
            for (ApplicationDocument document : files) {
                Button open = UiKit.secondary("Open " + document.getDocumentName());
                open.setOnAction(event -> openDocument(document));
                detail.getChildren().add(open);
            }
        }
    }

    private BigDecimal disbursed(Application application) {
        if (application.getApprovedAmount() == null) {
            return null;
        }
        return application.getApprovedAmount().subtract(context.funds().unpaid(application));
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
}
