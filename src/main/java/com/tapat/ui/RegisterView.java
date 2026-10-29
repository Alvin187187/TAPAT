package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.User;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegisterView {
    private final AppContext context;
    private final Session session;
    private final Navigator navigator;
    private final TextField fullName = UiKit.textField();
    private final TextField username = UiKit.textField();
    private final TextField email = UiKit.textField();
    private final TextField phone = UiKit.textField();
    private final TextField address = UiKit.textField();
    private final PasswordField password = UiKit.passwordField();
    private final Label error = UiKit.error();
    private final Parent root;

    public RegisterView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        this.navigator = navigator;
        Button create = UiKit.primary("Create account and enter");
        create.setDefaultButton(true);
        create.setMaxWidth(Double.MAX_VALUE);
        create.setOnAction(event -> submit());
        Button back = UiKit.text("Back to sign in");
        back.setOnAction(event -> navigator.showSignIn());
        VBox card = new VBox(12,
                UiKit.title("Create an applicant account"),
                UiKit.body("This account can apply and follow the case until the money is confirmed received. Staff accounts are created by an administrator."),
                UiKit.field("Full name", fullName),
                UiKit.field("Username", username),
                UiKit.field("Email", email),
                UiKit.field("Phone", phone),
                UiKit.field("Address", address),
                UiKit.field("Password (8+ characters)", password),
                error,
                create,
                back);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(520);
        card.setFillWidth(true);
        this.root = AuthLayout.shell(card);
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        fullName.clear();
        username.clear();
        email.clear();
        phone.clear();
        address.clear();
        password.clear();
        UiKit.clear(error);
    }

    private void submit() {
        UiKit.clear(error);
        try {
            User created = context.auth().registerApplicant(fullName.getText(), username.getText(), email.getText(),
                    phone.getText(), address.getText(), password.getText());
            User signedIn = context.auth().login(created.getUsername(), password.getText());
            session.signIn(signedIn);
            context.changed();
            navigator.showHome();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }
}
