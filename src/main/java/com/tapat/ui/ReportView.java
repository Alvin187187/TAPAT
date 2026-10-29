package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.Application;
import com.tapat.service.CaseReview;
import com.tapat.service.report.Report;

import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportView {
    private final AppContext context;
    private final Session session;
    private final ComboBox<Report> reports = new ComboBox<>();
    private final ComboBox<Application> traces = new ComboBox<>();
    private final VBox timeline = new VBox(8);
    private final TableView<List<String>> table = UiKit.table("This report has no rows.");
    private final PieChart chart = new PieChart();
    private final Label message = UiKit.meta("");
    private final Label error = UiKit.error();
    private final BorderPane root;
    private String pendingReference;

    public ReportView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        reports.setMaxWidth(480);
        reports.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Report report) {
                return report == null ? "" : report.getTitle();
            }

            @Override
            public Report fromString(String string) {
                return null;
            }
        });
        reports.setOnAction(event -> render(reports.getValue()));
        traces.setMaxWidth(480);
        traces.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Application application) {
                return application == null ? "" : application.getReferenceNo() + " — " + application.getApplicantName();
            }

            @Override
            public Application fromString(String string) {
                return null;
            }
        });
        traces.setOnAction(event -> showTimeline(traces.getValue()));
        timeline.getStyleClass().add("panel");
        UiKit.watch(timeline);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setPrefHeight(360);
        chart.setVisible(false);
        chart.setManaged(false);
        Button export = UiKit.primary("Export CSV");
        export.setOnAction(event -> export());
        VBox tableCard = UiKit.panel(UiKit.section("Amounts in PHP"), table);
        VBox main = new VBox(8, reports, message, error, tableCard, chart, export);
        main.setMinWidth(0);
        UiKit.grow(main);
        VBox side = new VBox(8, UiKit.section("Case timeline"), traces, timeline);
        side.setMinWidth(0);
        this.root = UiKit.workspace(
                UiKit.split(main, UiKit.sideScroll(side)),
                UiKit.title("Audit trail"),
                UiKit.body("Pick a case on the right. Status names are written in full. Amounts are PHP."));
    }

    public Parent getRoot() {
        return root;
    }

    public void selectReference(String reference) {
        this.pendingReference = reference;
    }

    public void refresh() {
        UiKit.clear(error);
        message.setText("");
        if (session.current().isEmpty()) {
            return;
        }
        try {
            List<Report> available = context.reports().auditorReports(session.current().get());
            reports.getItems().setAll(available);
            if (!available.isEmpty() && reports.getValue() == null) {
                reports.setValue(available.get(0));
                render(available.get(0));
            }
            Application selected = traces.getValue();
            List<Application> applications = context.applications().traceable(session.current().get());
            traces.getItems().setAll(applications);
            if (pendingReference != null) {
                String wanted = pendingReference;
                pendingReference = null;
                selected = applications.stream()
                        .filter(item -> wanted.equalsIgnoreCase(item.getReferenceNo()))
                        .findFirst()
                        .orElse(selected);
            }
            if (selected != null) {
                Application keep = selected;
                applications.stream().filter(item -> item.getId() == keep.getId()).findFirst()
                        .ifPresentOrElse(traces::setValue, () -> {
                            if (!applications.isEmpty()) {
                                traces.setValue(applications.get(0));
                            }
                        });
            } else if (!applications.isEmpty()) {
                traces.setValue(applications.get(0));
            }
            showTimeline(traces.getValue());
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private void render(Report report) {
        table.getColumns().clear();
        table.getItems().clear();
        chart.getData().clear();
        if (report == null) {
            return;
        }
        for (int index = 0; index < report.columns().size(); index++) {
            int columnIndex = index;
            boolean money = isMoney(report.columns().get(index));
            TableColumn<List<String>, String> column = UiKit.column(report.columns().get(index),
                    row -> display(row, columnIndex, money));
            if (index == 0) {
                column.setMinWidth(120);
                column.setPrefWidth(160);
            } else if (money) {
                column.getStyleClass().add("money-col");
                column.setMinWidth(80);
                column.setPrefWidth(96);
                column.setStyle("-fx-alignment: CENTER-RIGHT;");
            }
            table.getColumns().add(column);
        }
        table.getItems().setAll(report.rows());
        UiKit.fit(table);
        Map<String, Integer> slices = report.chartSlices();
        List<PieChart.Data> data = new ArrayList<>();
        for (Map.Entry<String, Integer> slice : slices.entrySet()) {
            if (slice.getValue() > 0) {
                data.add(new PieChart.Data(slice.getKey(), slice.getValue()));
            }
        }
        chart.getData().setAll(data);
        boolean showChart = !data.isEmpty();
        chart.setVisible(showChart);
        chart.setManaged(showChart);
    }

    private void export() {
        UiKit.clear(error);
        Report report = reports.getValue();
        if (report == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.setInitialFileName(report.getTitle().toLowerCase().replace(' ', '-') + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showSaveDialog(table.getScene() == null ? null : table.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            Files.writeString(Path.of(file.getAbsolutePath()), report.toCsv());
            message.setText("Saved to " + file.getAbsolutePath());
        } catch (Exception ex) {
            error.setText("Could not save the report.");
            error.setVisible(true);
            error.setManaged(true);
        }
    }

    private void showTimeline(Application application) {
        timeline.getChildren().clear();
        if (application == null) {
            timeline.getChildren().add(UiKit.body("Choose an application to read every step from submission to beneficiary confirmation."));
            return;
        }
        timeline.getChildren().add(UiKit.section(application.getReferenceNo()));
        timeline.getChildren().add(UiKit.badge(application.getStatus()));
        for (CaseReview.TraceLine line : context.cases().timeline(application)) {
            timeline.getChildren().add(UiKit.traceLine(line.when(), line.title(), line.detail()));
        }
    }

    private boolean isMoney(String column) {
        return column.equals("Allocated") || column.equals("Disbursed") || column.equals("Remaining")
                || column.equals("Requested") || column.equals("Approved") || column.equals("Released")
                || column.equals("Confirmed") || column.equals("Awaiting") || column.equals("Awaiting confirmation")
                || column.equals("Amount");
    }

    private String display(List<String> row, int index, boolean money) {
        if (index >= row.size()) {
            return "";
        }
        String value = row.get(index);
        if (money && value != null && value.startsWith("PHP ")) {
            return value.substring(4);
        }
        return value;
    }
}
