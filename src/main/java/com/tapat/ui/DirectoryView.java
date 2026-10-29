package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.Program;
import com.tapat.model.ProgramCategory;
import com.tapat.util.Money;
import com.tapat.util.Text;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class DirectoryView {
    private final AppContext context;
    private final Session session;
    private final Navigator navigator;
    private final TextField search = UiKit.textField();
    private final ComboBox<String> category = new ComboBox<>();
    private final TableView<Program> table = UiKit.table("No programs match this search.");
    private final VBox detail = new VBox(12);
    private final Label error = UiKit.error();
    private final Button apply = UiKit.primary("Apply");
    private final Label heading = UiKit.title("Apply");
    private final Label intro = UiKit.body("Pick one program. TAPAT will check documents and possible duplicates, then a person decides.");
    private final BorderPane root;
    private List<Program> programs = List.of();

    public DirectoryView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        this.navigator = navigator;
        search.setPromptText("Search programs");
        search.textProperty().addListener((obs, previous, current) -> applyFilter());
        category.setMinWidth(180);
        category.setPrefWidth(200);
        category.setMaxWidth(220);
        category.valueProperty().addListener((obs, previous, current) -> applyFilter());
        javafx.scene.control.TableColumn<Program, String> name = UiKit.column("Program", Program::getName);
        name.setMinWidth(140);
        name.setPrefWidth(200);
        javafx.scene.control.TableColumn<Program, String> kind = UiKit.column("Category", program -> program.getCategory().getLabel());
        kind.setMinWidth(110);
        kind.setPrefWidth(150);
        javafx.scene.control.TableColumn<Program, String> closes = UiKit.column("Closes", program ->
                program.getDeadline() == null ? "Open" : Text.date(program.getDeadline()));
        closes.setMinWidth(90);
        closes.setPrefWidth(110);
        table.getColumns().add(name);
        table.getColumns().add(kind);
        table.getColumns().add(closes);
        table.getColumns().add(UiKit.moneyColumn("Max PHP", program -> Money.compact(program.getMaxAssistance())));
        table.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> show(selected));
        apply.setOnAction(event -> apply());
        detail.getStyleClass().add("panel");
        UiKit.watch(detail);
        VBox list = new VBox(8, UiKit.toolbar(search, category), table);
        list.setMinWidth(0);
        table.setMinWidth(0);
        search.setPrefWidth(260);
        UiKit.grow(list);
        this.root = UiKit.workspace(
                UiKit.split(list, UiKit.sideScroll(detail)),
                heading,
                intro,
                error);
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        UiKit.clear(error);
        programs = context.programs().listOpen();
        String selectedCategory = category.getValue();
        List<String> categories = new ArrayList<>();
        categories.add("All categories");
        for (ProgramCategory item : ProgramCategory.values()) {
            categories.add(item.getLabel());
        }
        category.getItems().setAll(categories);
        category.setValue(selectedCategory == null ? "All categories" : selectedCategory);
        applyFilter();
        if (session.current().isEmpty()) {
            heading.setText("Programs");
            intro.setText("Public list of open programs. Sign in as an applicant to apply. Names are never shown here.");
            apply.setText("Sign in to apply");
            apply.setDisable(false);
        } else if (session.current().get().canSubmitApplications()) {
            heading.setText("Apply");
            intro.setText("Pick a program on the left, then Apply. A person reviews it. Nothing is auto-approved.");
            apply.setText("Apply");
            apply.setDisable(false);
        } else {
            heading.setText("Programs");
            intro.setText("Reference list of open programs. Your work is in the other menu items.");
            apply.setText("Apply");
            apply.setDisable(true);
        }
    }

    private void applyFilter() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        String chosen = category.getValue();
        List<Program> shown = new ArrayList<>();
        for (Program program : programs) {
            boolean textMatch = query.isEmpty()
                    || program.getName().toLowerCase().contains(query)
                    || program.getDescription().toLowerCase().contains(query);
            boolean categoryMatch = chosen == null || chosen.equals("All categories")
                    || program.getCategory().getLabel().equals(chosen);
            if (textMatch && categoryMatch) {
                shown.add(program);
            }
        }
        Program selected = table.getSelectionModel().getSelectedItem();
        table.getItems().setAll(shown);
        if (selected != null) {
            shown.stream().filter(program -> program.getId() == selected.getId()).findFirst()
                    .ifPresent(program -> table.getSelectionModel().select(program));
        }
        if (table.getSelectionModel().getSelectedItem() == null && !shown.isEmpty()) {
            table.getSelectionModel().select(0);
        }
        if (shown.isEmpty()) {
            show(null);
        }
    }

    private void show(Program program) {
        detail.getChildren().clear();
        boolean staff = session.current().isPresent() && !session.current().get().canSubmitApplications();
        apply.setDisable(program == null || staff);
        if (program == null) {
            detail.getChildren().add(UiKit.body("Nothing matches that search. Clear the search box to see every open program."));
            return;
        }
        detail.getChildren().add(UiKit.section(program.getName()));
        detail.getChildren().add(UiKit.finding(program.getCategory().getLabel() + " · " + Text.closes(program.getDeadline())));
        detail.getChildren().add(UiKit.fact("Maximum", Money.format(program.getMaxAssistance())));
        detail.getChildren().add(UiKit.fact("Income limit", program.getIncomeLimit() == null
                ? "None set" : Money.format(program.getIncomeLimit())));
        detail.getChildren().add(UiKit.section("What it covers"));
        detail.getChildren().add(UiKit.body(program.getDescription()));
        detail.getChildren().add(UiKit.section("Who can apply"));
        detail.getChildren().add(UiKit.body(program.getEligibilitySummary()));
        detail.getChildren().add(UiKit.section("Documents to attach"));
        if (program.getRequiredDocuments().isEmpty()) {
            detail.getChildren().add(UiKit.meta("This program does not ask for documents."));
        } else {
            for (String document : program.getRequiredDocuments()) {
                detail.getChildren().add(UiKit.body(document));
            }
        }
        if (program.getInstructions() != null && !program.getInstructions().isBlank()) {
            detail.getChildren().add(UiKit.section("How to apply"));
            detail.getChildren().add(UiKit.body(program.getInstructions()));
        }
        if (apply.getParent() instanceof javafx.scene.layout.Pane) {
            javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) apply.getParent();
            pane.getChildren().remove(apply);
        }
        apply.setMaxWidth(Double.MAX_VALUE);
        detail.getChildren().add(apply);
    }

    private void apply() {
        UiKit.clear(error);
        Program program = table.getSelectionModel().getSelectedItem();
        if (program == null) {
            return;
        }
        if (session.current().isEmpty()) {
            navigator.showSignIn();
            return;
        }
        if (!session.current().get().canSubmitApplications()) {
            error.setText("This account reviews or records funds. Use an applicant account to submit.");
            error.setVisible(true);
            error.setManaged(true);
            return;
        }
        navigator.openApplication(program.getId());
    }
}
