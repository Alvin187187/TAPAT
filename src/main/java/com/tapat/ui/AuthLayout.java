package com.tapat.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class AuthLayout {
    private AuthLayout() {
    }

    static Parent shell(Node form) {
        if (form instanceof Region) {
            Region region = (Region) form;
            region.setMaxWidth(520);
        }
        VBox wrap = new VBox(form);
        wrap.getStyleClass().add("auth-form");
        wrap.setAlignment(Pos.TOP_CENTER);
        wrap.setFillWidth(true);
        StackPane left = new StackPane(hero());
        left.getStyleClass().add("auth-hero-wrap");
        left.setAlignment(Pos.TOP_LEFT);
        left.setMinWidth(340);
        left.setPrefWidth(420);
        left.setMaxWidth(460);
        javafx.scene.control.ScrollPane scroll = UiKit.scroll(wrap);
        wrap.minWidthProperty().bind(scroll.widthProperty());
        BorderPane root = new BorderPane();
        root.setLeft(left);
        root.setCenter(scroll);
        root.getStyleClass().add("auth-shell");
        root.setMinSize(0, 0);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return root;
    }

    static VBox hero() {
        HBox brand = TapatLogo.wordmark(34, true);
        Label kicker = new Label("Community financial assistance");
        kicker.getStyleClass().add("auth-hero-kicker");

        Label headline = new Label("From apply to verified receipt.");
        headline.getStyleClass().add("auth-headline");
        headline.setWrapText(true);
        headline.setMaxWidth(360);

        Label body = new Label("TAPAT follows assistance from the application to the person who received it. Public totals never include names.");
        body.getStyleClass().add("auth-hero-body");
        body.setWrapText(true);
        body.setMaxWidth(360);

        Label path = new Label("What happens to a case");
        path.getStyleClass().add("auth-hero-kicker");

        Label note = new Label("Auditors sign in only to read that trail. They do not approve or release money.");
        note.getStyleClass().add("auth-hero-note");
        note.setWrapText(true);
        note.setMaxWidth(360);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox hero = new VBox(16,
                new VBox(6, brand, kicker),
                headline,
                body,
                path,
                step("1", "Apply", "Applicant submits a case for a program."),
                step("2", "Approve", "Administrator checks papers and duplicates, then reserves an amount."),
                step("3", "Release", "Finance issues a claim code when the money goes out."),
                step("4", "Verify", "The beneficiary presents the code. Only then is the case closed."),
                note,
                spacer);
        hero.getStyleClass().add("auth-hero");
        hero.setMaxWidth(Double.MAX_VALUE);
        hero.setMaxHeight(Double.MAX_VALUE);
        return hero;
    }

    private static HBox step(String number, String title, String text) {
        Label n = new Label(number);
        n.getStyleClass().add("auth-step-num");
        Label heading = new Label(title);
        heading.getStyleClass().add("auth-step-title");
        Label line = new Label(text);
        line.getStyleClass().add("auth-step");
        line.setWrapText(true);
        line.setMaxWidth(300);
        VBox copy = new VBox(2, heading, line);
        HBox row = new HBox(10, n, copy);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }
}
