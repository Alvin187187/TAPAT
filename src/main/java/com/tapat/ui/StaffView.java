package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.Role;
import com.tapat.model.User;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class StaffView {
    private final AppContext context;
    private final Session session;
    private final TextField fullName = UiKit.textField();
    private final TextField username = UiKit.textField();
    private final TextField email = UiKit.textField();
    private final TextField phone = UiKit.textField();
    private final PasswordField password = UiKit.passwordField();
    private final ComboBox<Role> role = UiKit.combo(List.of(Role.FINANCE_OFFICER, Role.AUDITOR));
    private final TableView<User> table = UiKit.table("No accounts yet.");
    private final Label error = UiKit.error();
    private final BorderPane root;

    public StaffView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        role.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Role item) {
                return item == null ? "" : item.getLabel();
            }

            @Override
            public Role fromString(String string) {
                return null;
            }
        });
        role.setValue(Role.FINANCE_OFFICER);
        table.getColumns().add(UiKit.column("Name", User::getFullName));
        table.getColumns().add(UiKit.column("Username", User::getUsername));
        table.getColumns().add(UiKit.column("Role", user -> user.getRole().getLabel()));
        table.getColumns().add(UiKit.column("Email", user -> user.getEmail() == null ? "" : user.getEmail()));
        Button create = UiKit.primary("Create account");
        create.setOnAction(event -> create());
        VBox form = new VBox(12,
                UiKit.section("New staff account"),
                UiKit.field("Full name", fullName),
                UiKit.field("Username", username),
                UiKit.field("Email", email),
                UiKit.field("Phone", phone),
                UiKit.field("Password", password),
                UiKit.field("Role", role),
                error,
                create);
        form.getStyleClass().addAll("form", "panel");
        UiKit.watch(form);
        VBox list = new VBox(8, table);
        list.setMinWidth(0);
        table.setMinWidth(0);
        UiKit.grow(list);
        this.root = UiKit.workspace(
                UiKit.split(list, UiKit.sideScroll(form)),
                UiKit.title("Staff"),
                UiKit.body("Create a finance officer or an auditor. Administrators are not created here."));
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
            table.getItems().setAll(context.auth().listUsers(session.current().get()));
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private void create() {
        UiKit.clear(error);
        if (session.current().isEmpty()) {
            return;
        }
        try {
            context.auth().registerStaff(session.current().get(), role.getValue(), fullName.getText(), username.getText(),
                    email.getText(), phone.getText(), password.getText());
            fullName.clear();
            username.clear();
            email.clear();
            phone.clear();
            password.clear();
            context.changed();
            refresh();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }
}
