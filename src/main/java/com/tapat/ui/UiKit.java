package com.tapat.ui;

import com.tapat.exception.TapatException;
import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;

import java.math.BigDecimal;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.function.Function;

public class UiKit {
    private UiKit() {
    }

    public static Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-title");
        readable(label);
        return label;
    }

    public static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        readable(label);
        return label;
    }

    public static Label body(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("body-text");
        readable(label);
        return label;
    }

    public static Label meta(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("meta");
        readable(label);
        return label;
    }

    public static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        readable(label);
        return label;
    }

    public static Label error() {
        Label label = new Label();
        label.getStyleClass().add("error-text");
        readable(label);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    public static VBox metric(String label, Label value) {
        value.getStyleClass().add("figure");
        readable(value);
        VBox box = new VBox(2, meta(label), value);
        box.getStyleClass().add("metric");
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static void readable(Label label) {
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Region.USE_PREF_SIZE);
    }

    public static void showError(Label label, Exception exception) {
        label.setText(message(exception));
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void clear(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    public static String message(Exception exception) {
        if (exception instanceof TapatException) {
            TapatException tapat = (TapatException) exception;
            if (tapat.getMessage() != null) {
                return tapat.getMessage();
            }
        }
        return "Something went wrong. Try again.";
    }

    public static TextField textField() {
        TextField field = new TextField();
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    public static PasswordField passwordField() {
        PasswordField field = new PasswordField();
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    public static TextArea textArea(int rows) {
        TextArea area = new TextArea();
        area.setWrapText(true);
        area.setPrefRowCount(rows);
        area.setMaxWidth(Double.MAX_VALUE);
        return area;
    }

    public static <T> ComboBox<T> combo(java.util.List<T> items) {
        ComboBox<T> combo = new ComboBox<>();
        combo.getItems().setAll(items);
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    public static DatePicker datePicker() {
        DatePicker picker = new DatePicker();
        picker.setMaxWidth(Double.MAX_VALUE);
        return picker;
    }

    public static VBox field(String label, Node input) {
        VBox box = new VBox(3, fieldLabel(label), input);
        box.setFillWidth(true);
        return box;
    }

    public static Button primary(String text) {
        return button(text, "button-primary");
    }

    public static Button secondary(String text) {
        return button(text, "button-secondary");
    }

    public static Button danger(String text) {
        return button(text, "button-danger");
    }

    public static Button text(String label) {
        return button(label, "button-text");
    }

    public static HBox actions(Node... nodes) {
        HBox box = new HBox(8, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static VBox page(Node... nodes) {
        VBox box = new VBox(12, nodes);
        box.getStyleClass().add("page");
        box.setFillWidth(true);
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    public static javafx.scene.layout.BorderPane workspace(Node body, Node... header) {
        VBox top = new VBox(8, header);
        top.setPadding(new Insets(16, 20, 8, 20));
        top.setMinWidth(0);
        top.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.BorderPane pane = new javafx.scene.layout.BorderPane();
        pane.setTop(top);
        pane.setCenter(body);
        pane.getStyleClass().add("shell");
        javafx.scene.layout.BorderPane.setMargin(body, new Insets(0, 20, 16, 20));
        if (body instanceof Region) {
            grow((Region) body);
        }
        return pane;
    }

    public static ScrollPane sideScroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinSize(0, 0);
        scroll.getStyleClass().add("scroll-pane");
        HBox.setHgrow(scroll, Priority.NEVER);
        return scroll;
    }

    public static void watch(Pane pane) {
        pane.widthProperty().addListener((obs, previous, width) -> wrapLabels(pane, width.doubleValue()));
        pane.getChildren().addListener((javafx.collections.ListChangeListener<Node>) change ->
                wrapLabels(pane, pane.getWidth()));
    }

    private static void wrapLabels(Pane pane, double width) {
        if (width <= 0) {
            return;
        }
        double inner = Math.max(80, width - 12);
        for (Node child : pane.getChildren()) {
            if (child instanceof Label) {
                Label label = (Label) child;
                label.setWrapText(true);
                label.setMinWidth(0);
                label.setMaxWidth(inner);
            }
        }
    }

    public static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinSize(0, 0);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        if (content instanceof Region) {
            Region region = (Region) content;
            region.setMinSize(0, 0);
            region.setMaxWidth(Double.MAX_VALUE);
            javafx.beans.value.ChangeListener<Object> fit = (obs, previous, value) -> {
                double viewport = scroll.getViewportBounds().getWidth();
                if (viewport < 40) {
                    viewport = scroll.getWidth();
                }
                if (viewport > 40) {
                    region.setPrefWidth(viewport);
                    region.setMaxWidth(viewport);
                }
            };
            scroll.viewportBoundsProperty().addListener(fit);
            scroll.widthProperty().addListener(fit);
        }
        return scroll;
    }

    public static void grow(Region region) {
        VBox.setVgrow(region, Priority.ALWAYS);
        HBox.setHgrow(region, Priority.ALWAYS);
        region.setMaxHeight(Double.MAX_VALUE);
        region.setMaxWidth(Double.MAX_VALUE);
    }

    public static HBox badge(ApplicationStatus status) {
        Label label = new Label(status.getLabel());
        label.getStyleClass().add("badge");
        label.setMaxWidth(Region.USE_PREF_SIZE);
        String badgeStyle = "badge-pending";
        if (status == ApplicationStatus.UNDER_REVIEW) {
            badgeStyle = "badge-review";
        } else if (status == ApplicationStatus.APPROVED) {
            badgeStyle = "badge-approved";
        } else if (status == ApplicationStatus.FOR_RELEASE) {
            badgeStyle = "badge-release";
        } else if (status == ApplicationStatus.RELEASED) {
            badgeStyle = "badge-released";
        } else if (status == ApplicationStatus.VERIFIED) {
            badgeStyle = "badge-verified";
        } else if (status == ApplicationStatus.REJECTED) {
            badgeStyle = "badge-rejected";
        }
        label.getStyleClass().add(badgeStyle);
        HBox wrap = new HBox(label);
        wrap.setAlignment(Pos.CENTER_LEFT);
        wrap.setMaxWidth(Double.MAX_VALUE);
        return wrap;
    }

    public static VBox traceLine(String when, String title, String detail) {
        Label time = meta(when);
        Label event = new Label(title);
        event.getStyleClass().add("trace-title");
        readable(event);
        VBox box = new VBox(2, time, event);
        if (detail != null && !detail.isBlank()) {
            box.getChildren().add(meta(detail));
        }
        box.getStyleClass().add("trace-line");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    public static HBox scoreLine(boolean passed, String name, String detail) {
        Label mark = new Label(passed ? "Pass" : "Flag");
        mark.getStyleClass().addAll("badge", passed ? "badge-verified" : "badge-pending");
        mark.setMaxWidth(Region.USE_PREF_SIZE);
        Label heading = new Label(name);
        heading.getStyleClass().add("trace-title");
        readable(heading);
        VBox copy = new VBox(2, heading, meta(detail));
        HBox.setHgrow(copy, Priority.ALWAYS);
        HBox row = new HBox(8, mark, copy);
        row.getStyleClass().add("score-line");
        row.setAlignment(Pos.TOP_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    public static VBox historyLine(String year, String program, String amount, String status, String person) {
        Label heading = new Label(program);
        heading.getStyleClass().add("trace-title");
        readable(heading);
        VBox box = new VBox(2, meta(year), heading, meta(amount + "  ·  " + status + "  ·  " + person));
        box.getStyleClass().add("history-line");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    public static <T> TableView<T> table(String empty) {
        TableView<T> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(meta(empty));
        table.setFixedCellSize(40);
        table.setMinWidth(0);
        table.setMaxWidth(Double.MAX_VALUE);
        table.getItems().addListener((javafx.collections.ListChangeListener<? super T>) change -> fit(table));
        fit(table);
        return table;
    }

    public static void fit(TableView<?> table) {
        int rows = table.getItems() == null ? 0 : table.getItems().size();
        double height = rows == 0 ? 96 : 36 + (rows * 40) + 4;
        table.setPrefHeight(height);
        table.setMinHeight(height);
        table.setMaxHeight(height);
    }

    public static <T> TableColumn<T, String> column(String title, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        column.setMinWidth(110);
        column.setPrefWidth(150);
        return column;
    }

    public static <T> TableColumn<T, String> moneyColumn(String title, Function<T, String> value) {
        TableColumn<T, String> column = column(title, value);
        column.getStyleClass().add("money-col");
        column.setMinWidth(80);
        column.setPrefWidth(96);
        column.setStyle("-fx-alignment: CENTER-RIGHT;");
        return column;
    }

    public static TableColumn<Application, ApplicationStatus> statusColumn() {
        TableColumn<Application, ApplicationStatus> column = new TableColumn<>("Status");
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatus()));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(ApplicationStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : badge(item));
            }
        });
        column.setPrefWidth(120);
        column.setMinWidth(100);
        return column;
    }

    public static boolean confirm(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }

    public static Insets pad(double value) {
        return new Insets(value);
    }

    public static HBox pageHeader(String title, Node... actions) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        Label heading = title(title);
        heading.setMinWidth(0);
        row.getChildren().add(heading);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().add(spacer);
        row.getChildren().addAll(actions);
        return row;
    }

    public static VBox panel(Node... nodes) {
        VBox box = new VBox(8, nodes);
        box.getStyleClass().add("panel");
        box.setFillWidth(true);
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        watch(box);
        return box;
    }

    public static HBox split(Region main, Region side) {
        main.setMinWidth(0);
        main.setMaxWidth(Double.MAX_VALUE);
        side.setMinWidth(280);
        side.setPrefWidth(340);
        side.setMaxWidth(380);
        HBox.setHgrow(main, Priority.ALWAYS);
        grow(main);
        HBox row = new HBox(12, main, side);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setFillHeight(true);
        grow(row);
        row.widthProperty().addListener((obs, previous, width) -> {
            double available = width.doubleValue();
            if (available <= 0) {
                return;
            }
            double sideWidth = Math.min(380, Math.max(280, available * 0.32));
            if (available < 760) {
                sideWidth = Math.min(340, Math.max(260, available * 0.40));
            }
            side.setPrefWidth(sideWidth);
            side.setMaxWidth(sideWidth);
            main.setPrefWidth(Math.max(0, available - sideWidth - 12));
            main.setMaxWidth(Double.MAX_VALUE);
        });
        return row;
    }

    public static HBox fact(String label, String value) {
        Label name = new Label(label);
        name.getStyleClass().add("fact-label");
        name.setMinWidth(110);
        name.setPrefWidth(110);
        Label amount = new Label(value == null || value.isBlank() ? "—" : value);
        amount.getStyleClass().add("fact-value");
        readable(amount);
        HBox row = new HBox(10, name, amount);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(amount, Priority.ALWAYS);
        return row;
    }

    public static Label finding(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("finding");
        readable(label);
        return label;
    }

    public static HBox toolbar(Node... nodes) {
        HBox row = new HBox(8, nodes);
        row.getStyleClass().add("toolbar");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static FlowPane assistanceTrack(ApplicationStatus status, BigDecimal approved, BigDecimal disbursed) {
        boolean rejected = status == ApplicationStatus.REJECTED;
        boolean inReview = status == ApplicationStatus.UNDER_REVIEW || status.isDecided();
        boolean reserved = status.commitsFunds();
        boolean released = status == ApplicationStatus.RELEASED || status == ApplicationStatus.VERIFIED;
        boolean verified = status == ApplicationStatus.VERIFIED;
        FlowPane track = new FlowPane(6, 6);
        track.setPrefWrapLength(320);
        addStep(track, "Submitted", true, status == ApplicationStatus.PENDING);
        addStep(track, "Under review", inReview, status == ApplicationStatus.UNDER_REVIEW);
        if (rejected) {
            addStep(track, "Rejected", true, true);
            return track;
        }
        addStep(track, "For release", reserved, status == ApplicationStatus.FOR_RELEASE || status == ApplicationStatus.APPROVED);
        addStep(track, "Released", released, status == ApplicationStatus.RELEASED);
        addStep(track, "Verified", verified, verified);
        return track;
    }

    private static void addStep(FlowPane track, String name, boolean reached, boolean current) {
        Label label = new Label(name);
        label.getStyleClass().add("track-step");
        if (current) {
            label.getStyleClass().add("track-step-current");
        } else if (reached) {
            label.getStyleClass().add("track-step-done");
        }
        track.getChildren().add(label);
    }

    private static Button button(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        return button;
    }
}
