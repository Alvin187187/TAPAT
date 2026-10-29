package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.Program;
import com.tapat.model.ProgramCategory;
import com.tapat.model.ProgramStatus;
import com.tapat.util.Money;
import com.tapat.util.Text;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

public class ProgramEditorView {
    private final AppContext context;
    private final Session session;
    private final TableView<Program> table = UiKit.table("No programs yet.");
    private final TextField name = UiKit.textField();
    private final ComboBox<ProgramCategory> category = UiKit.combo(java.util.List.of(ProgramCategory.values()));
    private final TextArea description = UiKit.textArea(3);
    private final TextArea eligibility = UiKit.textArea(3);
    private final TextArea documents = UiKit.textArea(3);
    private final TextArea instructions = UiKit.textArea(3);
    private final DatePicker deadline = UiKit.datePicker();
    private final TextField maximum = UiKit.textField();
    private final TextField incomeLimit = UiKit.textField();
    private final ComboBox<ProgramStatus> status = UiKit.combo(java.util.List.of(ProgramStatus.values()));
    private final Label error = UiKit.error();
    private final BorderPane root;
    private int editingId;
    private Integer createdBy;
    private LocalDateTime createdAt;

    public ProgramEditorView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        category.setConverter(label(ProgramCategory::getLabel));
        status.setConverter(label(ProgramStatus::getLabel));
        table.getColumns().add(UiKit.column("Program", Program::getName));
        table.getColumns().add(UiKit.column("Category", program -> program.getCategory().getLabel()));
        table.getColumns().add(UiKit.column("Status", program -> program.getStatus().getLabel()));
        table.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> {
            if (selected != null) {
                load(selected);
            }
        });
        Button save = UiKit.primary("Save program");
        save.setOnAction(event -> save());
        Button create = UiKit.secondary("New program");
        create.setOnAction(event -> clearForm());
        VBox form = new VBox(12,
                UiKit.section("Program details"),
                UiKit.field("Name", name),
                UiKit.field("Category", category),
                UiKit.field("Description", description),
                UiKit.field("Eligibility", eligibility),
                UiKit.field("Required documents", documents),
                UiKit.body("Write one document name on each line."),
                UiKit.field("Instructions", instructions),
                UiKit.field("Deadline", deadline),
                UiKit.field("Maximum assistance", maximum),
                UiKit.field("Income limit", incomeLimit),
                UiKit.field("Status", status),
                error,
                save);
        form.getStyleClass().addAll("form", "panel");
        UiKit.watch(form);
        VBox list = new VBox(8, table);
        list.setMinWidth(0);
        table.setMinWidth(0);
        UiKit.grow(list);
        this.root = UiKit.workspace(
                UiKit.split(list, UiKit.sideScroll(form)),
                UiKit.pageHeader("Programs", create),
                UiKit.body("Create what applicants can apply for. Only Open programs appear on Apply."));
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        UiKit.clear(error);
        if (session.current().isEmpty()) {
            return;
        }
        try {
            table.getItems().setAll(context.programs().listAll(session.current().get()));
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private void load(Program program) {
        editingId = program.getId();
        createdBy = program.getCreatedBy();
        createdAt = program.getCreatedAt();
        name.setText(program.getName());
        category.setValue(program.getCategory());
        description.setText(program.getDescription());
        eligibility.setText(program.getEligibilitySummary());
        documents.setText(Text.joinLines(program.getRequiredDocuments()));
        instructions.setText(Text.emptyIfNull(program.getInstructions()));
        deadline.setValue(program.getDeadline());
        maximum.setText(program.getMaxAssistance() == null ? "" : program.getMaxAssistance().toPlainString());
        incomeLimit.setText(program.getIncomeLimit() == null ? "" : program.getIncomeLimit().toPlainString());
        status.setValue(program.getStatus());
    }

    private void clearForm() {
        editingId = 0;
        createdBy = session.current().map(user -> user.getId()).orElse(null);
        createdAt = LocalDateTime.now().withNano(0);
        name.clear();
        category.setValue(null);
        description.clear();
        eligibility.clear();
        documents.clear();
        instructions.clear();
        deadline.setValue(null);
        maximum.clear();
        incomeLimit.clear();
        status.setValue(ProgramStatus.OPEN);
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        UiKit.clear(error);
        if (session.current().isEmpty()) {
            return;
        }
        try {
            if (editingId == 0) {
                createdBy = session.current().get().getId();
                createdAt = LocalDateTime.now().withNano(0);
            }
            Program program = new Program(
                    editingId,
                    name.getText().trim(),
                    category.getValue(),
                    description.getText().trim(),
                    eligibility.getText().trim(),
                    Text.lines(documents.getText()),
                    Text.trimToNull(instructions.getText()),
                    deadline.getValue(),
                    Money.parse(maximum.getText()),
                    Money.parse(incomeLimit.getText()),
                    status.getValue(),
                    createdBy,
                    createdAt);
            Program saved = context.programs().save(session.current().get(), program);
            context.changed();
            refresh();
            table.getItems().stream().filter(item -> item.getId() == saved.getId()).findFirst()
                    .ifPresent(item -> table.getSelectionModel().select(item));
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private <T> javafx.util.StringConverter<T> label(java.util.function.Function<T, String> text) {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : text.apply(object);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }
}
