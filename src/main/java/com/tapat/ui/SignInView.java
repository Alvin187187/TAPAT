package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.AppModule;
import com.tapat.model.User;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SignInView {
    private final AppContext context;
    private final Session session;
    private final Navigator navigator;
    private final TextField username = UiKit.textField();
    private final PasswordField password = UiKit.passwordField();
    private final Label error = UiKit.error();
    private final Parent root;

    public SignInView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        this.navigator = navigator;
        Button signIn = UiKit.primary("Sign in");
        signIn.setDefaultButton(true);
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setOnAction(event -> submit());
        Button register = UiKit.secondary("Create an applicant account");
        register.setMaxWidth(Double.MAX_VALUE);
        register.setOnAction(event -> navigator.showRegister());
        Button guest = UiKit.text("Browse public programs");
        guest.setOnAction(event -> navigator.show(AppModule.DIRECTORY));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(left, right);
        grid.add(demo("Applicant", "applicant", "applicant123"), 0, 0);
        grid.add(demo("Administrator", "admin", "admin123"), 1, 0);
        grid.add(demo("Finance", "finance", "finance123"), 0, 1);
        grid.add(demo("Auditor", "auditor", "auditor123"), 1, 1);

        Button duplicate = UiKit.text("Possible duplicate: juanm / juan1234");
        duplicate.setOnAction(event -> enter("juanm", "juan1234"));

        VBox card = new VBox(8,
                UiKit.title("Sign in"),
                UiKit.body("Applicants apply. Admins approve. Finance releases. Auditors read the trail."),
                UiKit.field("Username", username),
                UiKit.field("Password", password),
                error,
                signIn,
                register,
                guest,
                UiKit.section("Demo desks"),
                UiKit.meta("One click signs you in. Password is the username plus 123."),
                grid,
                duplicate);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(520);
        card.setFillWidth(true);
        this.root = AuthLayout.shell(card);
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        username.clear();
        password.clear();
        UiKit.clear(error);
    }

    private Button demo(String role, String name, String secret) {
        Button button = UiKit.secondary(role);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> enter(name, secret));
        return button;
    }

    private void enter(String name, String secret) {
        username.setText(name);
        password.setText(secret);
        submit();
    }

    private void submit() {
        UiKit.clear(error);
        try {
            User user = context.auth().login(username.getText(), password.getText());
            session.signIn(user);
            context.changed();
            navigator.showHome();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }
}
