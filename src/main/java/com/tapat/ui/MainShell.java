package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.AppModule;
import com.tapat.model.User;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;

public class MainShell {
    private final AppContext context;
    private final Session session;
    private final Navigator navigator;
    private final BorderPane root = new BorderPane();
    private final VBox sidebar;
    private final VBox navigation = new VBox();
    private final VBox footer = new VBox(4);
    private final Map<AppModule, Button> buttons = new EnumMap<>(AppModule.class);
    private AppModule current;
    private boolean authScreen = true;

    public MainShell(AppContext context, Session session) {
        this.context = context;
        this.session = session;
        BorderPane content = new BorderPane();
        content.setMinSize(0, 0);
        this.navigator = new Navigator(context, session, content, this::highlight);
        Label scope = new Label("Community assistance");
        scope.getStyleClass().add("meta");
        scope.setWrapText(true);
        VBox brandBlock = new VBox(4, TapatLogo.wordmark(28, true), scope);
        brandBlock.getStyleClass().add("brand-block");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        footer.getStyleClass().add("sidebar-user");
        navigation.setFillWidth(true);
        sidebar = new VBox(8, brandBlock, navigation, spacer, footer);
        sidebar.getStyleClass().add("sidebar");
        root.setCenter(content);
        root.setMinSize(0, 0);
        content.setMinSize(0, 0);
        sidebar.setMinHeight(0);
        root.getStyleClass().add("shell");
        context.setOnDataChanged(this::onDataChanged);
    }

    public Parent getRoot() {
        return root;
    }

    public void start() {
        navigator.showSignIn();
    }

    private void onDataChanged() {
        fillNav();
        applyHighlight();
    }

    private void fillNav() {
        navigation.setSpacing(4);
        navigation.getChildren().clear();
        buttons.clear();
        footer.getChildren().clear();
        if (authScreen) {
            root.setLeft(null);
            return;
        }
        root.setLeft(sidebar);
        for (AppModule module : session.modules()) {
            Button button = new Button(label(module));
            button.getStyleClass().add("nav-item");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setWrapText(true);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setOnAction(event -> navigator.show(module));
            buttons.put(module, button);
            navigation.getChildren().add(button);
        }
        if (session.current().isEmpty()) {
            footer.getChildren().add(UiKit.meta("Browsing as guest. Names are hidden."));
            Button signIn = UiKit.primary("Sign in");
            signIn.setMaxWidth(Double.MAX_VALUE);
            signIn.setOnAction(event -> navigator.showSignIn());
            footer.getChildren().add(signIn);
        } else {
            User user = session.current().get();
            footer.getChildren().add(UiKit.body(user.getFullName()));
            footer.getChildren().add(UiKit.meta(job(user)));
            Button signOut = UiKit.text("Sign out");
            signOut.setOnAction(event -> {
                session.signOut();
                navigator.showSignIn();
            });
            footer.getChildren().add(signOut);
        }
    }

    private String job(User user) {
        if (user.canReviewApplications()) {
            return "Approves cases";
        }
        if (user.canRecordFunds()) {
            return "Releases money";
        }
        if (user.canExportFullReports()) {
            return "Reads the audit trail";
        }
        if (user.canSubmitApplications()) {
            return "Applicant";
        }
        return user.getRole().getLabel();
    }

    private String label(AppModule module) {
        if (module == AppModule.NOTICES) {
            int unread = context.notifications().unread(session.current().orElse(null));
            return unread > 0 ? "Inbox  " + unread : "Inbox";
        }
        if (module == AppModule.DIRECTORY) {
            if (session.current().isEmpty()) {
                return "Programs";
            }
            if (session.current().get().canSubmitApplications()) {
                return "Apply";
            }
            return "Open programs";
        }
        return module.getLabel();
    }

    private void highlight(AppModule module) {
        boolean nextAuth = module == null && session.current().isEmpty();
        current = module == null ? current : module;
        if (nextAuth != authScreen) {
            authScreen = nextAuth;
            fillNav();
        } else if (!nextAuth && buttons.isEmpty()) {
            fillNav();
        } else {
            authScreen = nextAuth;
        }
        applyHighlight();
    }

    private void applyHighlight() {
        buttons.forEach((item, button) -> {
            if (item == current) {
                if (!button.getStyleClass().contains("active")) {
                    button.getStyleClass().add("active");
                }
            } else {
                button.getStyleClass().remove("active");
            }
        });
    }
}
