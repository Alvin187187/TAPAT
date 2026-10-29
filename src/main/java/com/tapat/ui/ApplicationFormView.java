package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.AppModule;
import com.tapat.model.Program;
import com.tapat.service.eligibility.EligibilityReport;
import com.tapat.util.Money;
import com.tapat.util.Text;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApplicationFormView {
    private final AppContext context;
    private final Session session;
    private final Navigator navigator;
    private final Label programName = UiKit.section("");
    private final Label programFacts = UiKit.meta("");
    private final TextField income = UiKit.textField();
    private final TextField requested = UiKit.textField();
    private final TextArea purpose = UiKit.textArea(4);
    private final VBox documents = new VBox(8);
    private final VBox check = new VBox(8);
    private final Label error = UiKit.error();
    private final Map<String, Path> files = new LinkedHashMap<>();
    private final BorderPane root;
    private Program program;
    private int programId;

    public ApplicationFormView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        this.navigator = navigator;
        income.textProperty().addListener((obs, previous, current) -> refreshCheck());
        requested.textProperty().addListener((obs, previous, current) -> refreshCheck());
        Button submit = UiKit.primary("Submit application");
        submit.setOnAction(event -> submit());
        Button back = UiKit.text("Back");
        back.setOnAction(event -> navigator.show(AppModule.MY_APPLICATIONS));
        VBox form = UiKit.panel(
                programName,
                programFacts,
                UiKit.field("Monthly household income", income),
                UiKit.body("Income is only a flag for the reviewer. It never auto-rejects you."),
                UiKit.field("Requested amount", requested),
                UiKit.field("What the money is for", purpose),
                UiKit.section("Documents"),
                documents,
                error,
                submit);
        VBox side = UiKit.panel(
                UiKit.section("Check before you submit"),
                UiKit.body("Flags go to the reviewer. This check never approves or rejects the case."),
                check);
        form.setMaxWidth(Double.MAX_VALUE);
        form.setMinWidth(0);
        this.root = UiKit.workspace(
                UiKit.split(form, UiKit.sideScroll(side)),
                UiKit.pageHeader("Apply", back));
    }

    public Parent getRoot() {
        return root;
    }

    public void open(int programId) {
        this.programId = programId;
        refresh();
    }

    public void refresh() {
        UiKit.clear(error);
        income.clear();
        requested.clear();
        purpose.clear();
        files.clear();
        documents.getChildren().clear();
        program = null;
        if (programId == 0) {
            programName.setText("Choose a program from the directory.");
            programFacts.setText("");
            refreshCheck();
            return;
        }
        try {
            program = context.programs().require(programId);
            programName.setText(program.getName());
            programFacts.setText(program.getCategory().getLabel()
                    + " · " + Text.closes(program.getDeadline())
                    + (program.getMaxAssistance() == null ? "" : " · maximum " + Money.format(program.getMaxAssistance())));
            requested.setPromptText(program.getMaxAssistance() == null ? "" : "Up to " + Money.format(program.getMaxAssistance()));
            for (String required : program.getRequiredDocuments()) {
                Label chosen = UiKit.meta("No file chosen");
                Button choose = UiKit.secondary("Choose file");
                choose.setOnAction(event -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle(required);
                    File file = chooser.showOpenDialog(documents.getScene() == null ? null : documents.getScene().getWindow());
                    if (file != null) {
                        files.put(required, file.toPath());
                        chosen.setText(file.getName());
                        refreshCheck();
                    }
                });
                documents.getChildren().add(new VBox(4, UiKit.fieldLabel(required), new HBox(8, choose, chosen)));
            }
            if (program.getRequiredDocuments().isEmpty()) {
                documents.getChildren().add(UiKit.meta("This program does not ask for documents."));
            }
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
        refreshCheck();
    }

    private void refreshCheck() {
        check.getChildren().clear();
        if (program == null || session.current().isEmpty()) {
            check.getChildren().add(UiKit.meta("Sign in and choose a program to see the check."));
            return;
        }
        try {
            BigDecimal household = Money.parse(income.getText());
            EligibilityReport report = context.applications().preview(
                    session.current().get(), program.getId(), household, List.copyOf(files.keySet()));
            if (report.getMessages().isEmpty()) {
                check.getChildren().add(UiKit.body("Nothing is flagged on the rules this program uses."));
            } else {
                check.getChildren().add(UiKit.finding("A reviewer will see these flags:"));
                for (String message : report.getMessages()) {
                    Label line = UiKit.body(message);
                    line.getStyleClass().add("error-text");
                    check.getChildren().add(line);
                }
            }
            BigDecimal amount = Money.parse(requested.getText());
            if (amount != null && program.getMaxAssistance() != null && amount.compareTo(program.getMaxAssistance()) > 0) {
                Label line = UiKit.body("Requested amount is above the maximum of " + Money.format(program.getMaxAssistance()) + ".");
                line.getStyleClass().add("error-text");
                check.getChildren().add(line);
            }
        } catch (RuntimeException ex) {
            check.getChildren().add(UiKit.meta(UiKit.message(ex)));
        }
    }

    private void submit() {
        UiKit.clear(error);
        if (session.current().isEmpty()) {
            navigator.showSignIn();
            return;
        }
        try {
            BigDecimal household = Money.parse(income.getText());
            BigDecimal amount = Money.requirePositive(requested.getText(), "requested amount");
            context.applications().submit(session.current().get(), programId, household, amount, purpose.getText(), files);
            context.changed();
            navigator.show(AppModule.MY_APPLICATIONS);
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }
}
