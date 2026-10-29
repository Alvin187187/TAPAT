package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.Application;
import com.tapat.model.FundEntry;
import com.tapat.model.Program;
import com.tapat.model.ProgramBalance;
import com.tapat.util.Money;
import com.tapat.util.Text;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

public class FinanceView {
    private final AppContext context;
    private final Session session;
    private final TableView<ProgramBalance> balances = UiKit.table("No balances yet.");
    private final ComboBox<Program> program = new ComboBox<>();
    private final TextField allocationAmount = UiKit.textField();
    private final TextField allocationRemarks = UiKit.textField();
    private final ComboBox<Application> payable = new ComboBox<>();
    private final TextField disbursementAmount = UiKit.textField();
    private final TextField disbursementRemarks = UiKit.textField();
    private final TextField claimReference = UiKit.textField();
    private final TableView<FundEntry> ledger = UiKit.table("No fund entries yet.");
    private final Label guard = UiKit.finding("");
    private final Label error = UiKit.error();
    private final BorderPane root;
    private String pendingClaim;
    private Integer pendingPayableId;

    public FinanceView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        balances.getColumns().add(UiKit.column("Program", ProgramBalance::getProgramName));
        balances.getColumns().add(UiKit.moneyColumn("Allocated", row -> Money.compact(row.getAllocated())));
        balances.getColumns().add(UiKit.moneyColumn("Disbursed", row -> Money.compact(row.getDisbursed())));
        balances.getColumns().add(UiKit.moneyColumn("Remaining", row -> Money.compact(row.getRemaining())));
        VBox balanceCard = UiKit.panel(UiKit.section("Program balances (PHP)"), balances);
        program.setMaxWidth(Double.MAX_VALUE);
        program.setConverter(new StringConverter<>() {
            @Override
            public String toString(Program item) {
                return item == null ? "" : item.getName();
            }

            @Override
            public Program fromString(String string) {
                return null;
            }
        });
        payable.setMaxWidth(Double.MAX_VALUE);
        payable.setConverter(new StringConverter<>() {
            @Override
            public String toString(Application item) {
                if (item == null) {
                    return "";
                }
                return item.getReferenceNo() + " — " + item.getApplicantName() + " — unpaid "
                        + Money.format(context.funds().unpaid(item));
            }

            @Override
            public Application fromString(String string) {
                return null;
            }
        });
        payable.valueProperty().addListener((obs, previous, current) -> updateGuard());
        disbursementAmount.textProperty().addListener((obs, previous, current) -> updateGuard());
        ledger.getColumns().add(UiKit.column("Date", entry -> Text.dateTime(entry.getRecordedAt())));
        ledger.getColumns().add(UiKit.column("Type", entry -> entry.getEntryType().getLabel()));
        ledger.getColumns().add(UiKit.column("Program", FundEntry::getProgramName));
        ledger.getColumns().add(UiKit.column("Reference", entry -> Text.emptyIfNull(entry.getApplicationReference())));
        ledger.getColumns().add(UiKit.moneyColumn("Amount", entry -> Money.compact(entry.getAmount())));
        ledger.getColumns().add(UiKit.column("Recorded by", FundEntry::getRecordedByName));
        Button allocate = UiKit.primary("Record allocation");
        allocate.setOnAction(event -> allocate());
        Button disburse = UiKit.primary("Release funds");
        disburse.setOnAction(event -> disburse());
        Button verify = UiKit.secondary("Verify claim");
        verify.setOnAction(event -> verifyClaim());
        VBox allocation = new VBox(8,
                UiKit.section("Record allocation"),
                UiKit.body("Put money into a program before you release any case."),
                UiKit.field("Program", program),
                UiKit.field("Amount", allocationAmount),
                UiKit.field("Remarks", allocationRemarks),
                allocate);
        allocation.getStyleClass().add("panel");
        allocation.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(allocation, Priority.ALWAYS);
        VBox disbursement = new VBox(8,
                UiKit.section("Record disbursement"),
                UiKit.body("Release an approved case. TAPAT issues a claim code. Verify that code when the person presents it."),
                UiKit.field("Approved application", payable),
                UiKit.field("Amount", disbursementAmount),
                guard,
                UiKit.field("Remarks", disbursementRemarks),
                disburse,
                UiKit.section("Proof of assistance"),
                UiKit.field("Claim reference", claimReference),
                verify);
        disbursement.getStyleClass().add("panel");
        disbursement.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(disbursement, Priority.ALWAYS);
        HBox forms = new HBox(12, allocation, disbursement);
        VBox ledgerCard = UiKit.panel(UiKit.section("Ledger (PHP)"), ledger);
        VBox body = new VBox(12, balanceCard, forms, error, ledgerCard);
        body.setMinWidth(0);
        this.root = UiKit.workspace(
                UiKit.scroll(body),
                UiKit.title("Release money"),
                UiKit.body("1. Put money in a program. 2. Release an approved case. 3. When the person shows the claim code, Verify claim."));
    }

    public Parent getRoot() {
        return root;
    }

    public void selectClaim(String claim) {
        this.pendingClaim = claim;
    }

    public void selectPayable(int applicationId) {
        this.pendingPayableId = applicationId;
    }

    public void refresh() {
        UiKit.clear(error);
        if (session.current().isEmpty()) {
            return;
        }
        try {
            balances.getItems().setAll(context.funds().summaries());
            List<Program> programs = context.programs().listOpen();
            program.getItems().setAll(programs);
            payable.getItems().setAll(context.funds().payable(session.current().get()));
            ledger.getItems().setAll(context.funds().ledger(session.current().get()));
            if (pendingClaim != null && !pendingClaim.isBlank()) {
                claimReference.setText(pendingClaim);
                pendingClaim = null;
            }
            if (pendingPayableId != null) {
                int wanted = pendingPayableId;
                pendingPayableId = null;
                payable.getItems().stream().filter(item -> item.getId() == wanted).findFirst()
                        .ifPresent(payable::setValue);
            }
            updateGuard();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private void updateGuard() {
        Application application = payable.getValue();
        if (application == null) {
            guard.setText("Choose an approved application to see what can still be released.");
            guard.getStyleClass().remove("error-text");
            return;
        }
        java.math.BigDecimal unpaid = context.funds().unpaid(application);
        java.math.BigDecimal remaining = context.funds().summaries().stream()
                .filter(row -> row.getProgramId() == application.getProgramId())
                .map(ProgramBalance::getRemaining)
                .findFirst()
                .orElse(java.math.BigDecimal.ZERO);
        String base = "Still unpaid " + Money.format(unpaid) + ". " + application.getProgramName()
                + " has " + Money.format(remaining) + " left.";
        try {
            java.math.BigDecimal typed = Money.parse(disbursementAmount.getText());
            if (typed != null && (typed.compareTo(unpaid) > 0 || typed.compareTo(remaining) > 0)) {
                guard.setText(base + " This amount cannot be recorded.");
                if (!guard.getStyleClass().contains("error-text")) {
                    guard.getStyleClass().add("error-text");
                }
                return;
            }
        } catch (RuntimeException ex) {
            guard.setText("Enter the amount as a number, for example 3000.");
            if (!guard.getStyleClass().contains("error-text")) {
                guard.getStyleClass().add("error-text");
            }
            return;
        }
        guard.getStyleClass().remove("error-text");
        guard.setText(base);
    }

    private void allocate() {
        UiKit.clear(error);
        if (session.current().isEmpty() || program.getValue() == null) {
            error.setText("Choose a program.");
            error.setVisible(true);
            error.setManaged(true);
            return;
        }
        try {
            context.funds().allocate(session.current().get(), program.getValue().getId(),
                    Money.requirePositive(allocationAmount.getText(), "amount"), allocationRemarks.getText());
            allocationAmount.clear();
            allocationRemarks.clear();
            context.changed();
            refresh();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private void disburse() {
        UiKit.clear(error);
        if (session.current().isEmpty() || payable.getValue() == null) {
            error.setText("Choose an approved application.");
            error.setVisible(true);
            error.setManaged(true);
            return;
        }
        try {
            int applicationId = payable.getValue().getId();
            context.funds().disburse(session.current().get(), applicationId,
                    Money.requirePositive(disbursementAmount.getText(), "amount"), disbursementRemarks.getText());
            disbursementAmount.clear();
            disbursementRemarks.clear();
            Application released = context.applications().require(applicationId);
            if (released.getClaimReference() != null) {
                claimReference.setText(released.getClaimReference());
                guard.setText(released.getClaimReference() + " is waiting for the beneficiary. Verify the claim when they present it.");
            }
            context.changed();
            refresh();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private void verifyClaim() {
        UiKit.clear(error);
        if (session.current().isEmpty()) {
            return;
        }
        try {
            Application verified = context.funds().verifyClaim(session.current().get(), claimReference.getText());
            guard.setText(verified.getClaimReference() + " is verified. "
                    + Money.format(verified.getApprovedAmount()) + " was confirmed received.");
            claimReference.clear();
            context.changed();
            refresh();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }
}
