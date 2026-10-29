package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.Program;
import com.tapat.model.ProgramBalance;
import com.tapat.util.Money;
import com.tapat.util.Text;

import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FundSummaryView {
    private final AppContext context;
    private final TableView<ProgramBalance> table = UiKit.table("No fund records yet.");
    private final Label heroFigure = new Label("PHP 0.00");
    private final Label heroNote = new Label("");
    private final Label allocatedFigure = new Label("PHP 0.00");
    private final Label confirmedFigure = new Label("PHP 0.00");
    private final Label awaitingFigure = new Label("PHP 0.00");
    private final VBox trace = new VBox(10);
    private final BarChart<String, Number> chart;
    private final BorderPane root;

    public FundSummaryView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        javafx.scene.control.TableColumn<ProgramBalance, String> program = UiKit.column("Program", ProgramBalance::getProgramName);
        program.setMinWidth(140);
        program.setPrefWidth(180);
        table.getColumns().add(program);
        table.getColumns().add(UiKit.moneyColumn("Allocated", row -> Money.compact(row.getAllocated())));
        table.getColumns().add(UiKit.moneyColumn("Approved", row -> Money.compact(row.getApprovedTotal())));
        table.getColumns().add(UiKit.moneyColumn("Released", row -> Money.compact(row.getDisbursed())));
        table.getColumns().add(UiKit.moneyColumn("Confirmed", row -> Money.compact(row.getConfirmedTotal())));
        table.getColumns().add(UiKit.moneyColumn("Awaiting", row -> Money.compact(row.getAwaitingTotal())));
        table.getColumns().add(UiKit.moneyColumn("Remaining", row -> Money.compact(row.getRemaining())));
        table.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> showTrace(selected));
        CategoryAxis categories = new CategoryAxis();
        NumberAxis amounts = new NumberAxis();
        amounts.setLabel("PHP");
        chart = new BarChart<>(categories, amounts);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(180);
        chart.setMinHeight(160);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.setCategoryGap(18);
        chart.setTitle(null);
        heroFigure.getStyleClass().add("hero-figure");
        heroFigure.setWrapText(true);
        heroNote.getStyleClass().add("hero-note");
        heroNote.setWrapText(true);
        heroNote.setMinWidth(0);
        heroNote.setMaxWidth(Double.MAX_VALUE);
        Label kicker = new Label("Still unspent");
        kicker.getStyleClass().add("hero-kicker");
        VBox hero = new VBox(4, kicker, heroFigure, heroNote);
        hero.getStyleClass().add("hero");
        hero.setMaxWidth(Double.MAX_VALUE);
        HBox metrics = new HBox(10,
                UiKit.metric("Allocated", allocatedFigure),
                UiKit.metric("Confirmed received", confirmedFigure),
                UiKit.metric("Awaiting confirmation", awaitingFigure));
        trace.getStyleClass().add("panel");
        UiKit.watch(trace);
        VBox tablePane = new VBox(8, UiKit.meta("Amounts in PHP"), table, chart);
        tablePane.setMinWidth(0);
        UiKit.grow(tablePane);
        VBox top = new VBox(10, hero, metrics);
        top.setMaxWidth(Double.MAX_VALUE);
        this.root = UiKit.workspace(
                UiKit.split(tablePane, UiKit.sideScroll(trace)),
                UiKit.title("Public funds"),
                UiKit.body("Allocated → approved → released → confirmed received. No applicant names."),
                top);
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        List<ProgramBalance> rows = context.funds().summaries();
        table.getItems().setAll(rows);
        BigDecimal remaining = BigDecimal.ZERO;
        BigDecimal allocated = BigDecimal.ZERO;
        BigDecimal confirmed = BigDecimal.ZERO;
        BigDecimal awaiting = BigDecimal.ZERO;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ProgramBalance row : rows) {
            remaining = remaining.add(row.getRemaining());
            allocated = allocated.add(row.getAllocated());
            confirmed = confirmed.add(row.getConfirmedTotal());
            awaiting = awaiting.add(row.getAwaitingTotal());
            series.getData().add(new XYChart.Data<>(row.getProgramName(), row.getRemaining().doubleValue()));
        }
        heroFigure.setText(Money.format(remaining));
        heroNote.setText(finding(rows, remaining));
        allocatedFigure.setText(Money.format(allocated));
        confirmedFigure.setText(Money.format(confirmed));
        awaitingFigure.setText(Money.format(awaiting));
        chart.getData().setAll(series);
        ProgramBalance selected = table.getSelectionModel().getSelectedItem();
        if (selected == null && !rows.isEmpty()) {
            table.getSelectionModel().select(0);
        } else {
            showTrace(selected);
        }
    }

    private void showTrace(ProgramBalance row) {
        trace.getChildren().clear();
        if (row == null) {
            trace.getChildren().add(UiKit.body("Select a program to see how its allocation moved from approval to beneficiary confirmation."));
            return;
        }
        trace.getChildren().add(UiKit.section(row.getProgramName()));
        trace.getChildren().add(UiKit.fact("Original allocation", Money.format(row.getAllocated())));
        trace.getChildren().add(UiKit.fact("Approved", Money.format(row.getApprovedTotal())));
        trace.getChildren().add(UiKit.fact("Released", Money.format(row.getDisbursed())));
        trace.getChildren().add(UiKit.fact("Beneficiary confirmed", Money.format(row.getConfirmedTotal())));
        trace.getChildren().add(UiKit.fact("Awaiting confirmation", Money.format(row.getAwaitingTotal())));
        trace.getChildren().add(UiKit.fact("Remaining", Money.format(row.getRemaining())));
        trace.getChildren().add(UiKit.fact("Still unreserved", Money.format(row.getUnreserved())));
    }

    private String finding(List<ProgramBalance> rows, BigDecimal remaining) {
        Program soonest = null;
        for (Program program : context.programs().listOpen()) {
            if (program.getDeadline() == null || program.getDeadline().isBefore(LocalDate.now())) {
                continue;
            }
            if (soonest == null || program.getDeadline().isBefore(soonest.getDeadline())) {
                soonest = program;
            }
        }
        if (soonest == null) {
            return "No open program has a closing date ahead of today. " + Money.format(remaining) + " remains on the books.";
        }
        Program match = soonest;
        BigDecimal left = rows.stream()
                .filter(row -> row.getProgramId() == match.getId())
                .map(ProgramBalance::getRemaining)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        return soonest.getName() + " " + Text.closes(soonest.getDeadline())
                + ". " + Money.format(left) + " of that program is still available.";
    }
}
